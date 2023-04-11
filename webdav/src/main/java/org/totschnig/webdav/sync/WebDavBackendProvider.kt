package org.totschnig.webdav.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import at.bitfire.dav4android.DavResource
import at.bitfire.dav4android.LockableDavResource
import at.bitfire.dav4android.exception.DavException
import at.bitfire.dav4android.exception.HttpException
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.source
import org.acra.util.StreamReader
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SequenceNumber
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.getSyncProviderUrl
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.io.calculateSize
import org.totschnig.myexpenses.util.io.getMimeType
import org.totschnig.webdav.sync.client.CertificateHelper.fromString
import org.totschnig.webdav.sync.client.InvalidCertificateException
import org.totschnig.webdav.sync.client.WebDavClient
import java.io.IOException
import java.io.InputStream
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class WebDavBackendProvider @SuppressLint("MissingPermission") internal constructor(
    context: Context,
    account: Account,
    accountManager: AccountManager
) : AbstractSyncBackendProvider<DavResource>(context) {

    private var webDavClient: WebDavClient
    private val fallbackToClass1: Boolean

    @Throws(IOException::class)
    override fun withAccount(account: org.totschnig.myexpenses.model2.Account) {
        setAccountUuid(account)
        webDavClient.mkCol(accountUuid)
        writeAccount(account, false)
    }

    @Throws(IOException::class)
    override fun writeAccount(account: org.totschnig.myexpenses.model2.Account, update: Boolean) {
        val accountMetadataFilename = accountMetadataFilename
        val metaData = webDavClient.getResource(accountMetadataFilename, accountUuid)
        if (update || !metaData.exists()) {
            saveFileContents(
                true,
                null,
                accountMetadataFilename,
                buildMetadata(account),
                mimeTypeForData,
                true
            )
            if (!update) {
                createWarningFile()
            }
        }
    }

    override fun readAccountMetaData(): Result<AccountMetaData> {
        return getAccountMetaDataFromDavResource(
            webDavClient.getResource(
                accountMetadataFilename,
                accountUuid
            )
        )
    }

    @Throws(IOException::class)
    override fun resetAccountData(uuid: String) {
        try {
            for (davResource in webDavClient.getFolderMembers(uuid)) {
                davResource.delete(null)
            }
        } catch (e: HttpException) {
            throw IOException(e)
        }
    }

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ): String? = readResourceIfExists(
        if (fromAccountDir)
            webDavClient.getResource(fileName, accountUuid)
        else
            webDavClient.getResource(fileName), maybeDecrypt)

    @Throws(IOException::class)
    private fun readResourceIfExists(resource: LockableDavResource, maybeDecrypt: Boolean): String? {
        return if (resource.exists()) {
            try {
               StreamReader(maybeDecrypt(resource["text/plain"].byteStream(), maybeDecrypt)).read()
            } catch (e: HttpException) {
                throw IOException(e)
            } catch (e: DavException) {
                throw IOException(e)
            }
        } else {
            null
        }
    }

    @Throws(IOException::class)
    override fun lock() {
        if (fallbackToClass1) {
            super.lock()
        } else {
            if (!webDavClient.lock(accountUuid)) {
                throw IOException("Backend cannot be locked")
            }
        }
    }

    override fun deleteLockTokenFile() {
        try {
            lockFile.delete(null)
        } catch (e: HttpException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun unlock() {
        if (fallbackToClass1) {
            super.unlock()
        } else {
            if (!webDavClient.unlock(accountUuid)) {
                throw IOException("Error while unlocking backend")
            }
        }
    }

    private val lockFile: LockableDavResource
        get() = webDavClient.getResource(LOCK_FILE, accountUuid)

    override fun getChangeSetFromResource(shardNumber: Int, resource: DavResource): ChangeSet {
        return try {
            getChangeSetFromInputStream(
                SequenceNumber(
                    shardNumber,
                    getSequenceFromFileName(resource.fileName())
                ),
                resource[mimeTypeForData].byteStream()
            )
        } catch (e: HttpException) {
            throw IOException(e)
        } catch (e: DavException) {
            throw IOException(e)
        }
    }

    override fun collectionForShard(shardNumber: Int) =
        if (shardNumber == 0) webDavClient.getCollection(accountUuid)
        else webDavClient.getCollection(folderForShard(shardNumber), accountUuid).takeIf { it.exists() }

    override fun childrenForCollection(folder: DavResource?): Set<DavResource> =
        if (folder != null) webDavClient.getFolderMembers(folder) else webDavClient.getFolderMembers(accountUuid)

    override fun nameForResource(resource: DavResource): String? = resource.fileName()

    override fun isCollection(resource: DavResource) = LockableDavResource.isCollection(resource)

    override val sharedPreferencesName = "webdav"

    @get:Throws(IOException::class)
    override val isEmpty: Boolean
        get() = webDavClient.getFolderMembers().isEmpty()

    @Throws(IOException::class)
    override fun getInputStreamForPicture(relativeUri: String): InputStream {
        return getInputStream(accountUuid!!, relativeUri)
    }

    @Throws(IOException::class)
    override fun getInputStreamForBackup(backupFile: String): InputStream {
        return getInputStream(BACKUP_FOLDER_NAME, backupFile)
    }

    @Throws(IOException::class)
    private fun getInputStream(folderName: String, resourceName: String): InputStream {
        return try {
            webDavClient.getResource(resourceName, folderName)["*/*"].byteStream()
        } catch (e: HttpException) {
            throw IOException(e)
        } catch (e: DavException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun saveUriToAccountDir(fileName: String, uri: Uri) {
        saveUriToFolder(fileName, uri, accountUuid!!, true)
    }

    @Throws(IOException::class)
    private fun saveUriToFolder(fileName: String, uri: Uri, folder: String, maybeEncrypt: Boolean) {
        val finalFileName = getLastFileNamePart(fileName)
        val encrypt = isEncrypted && maybeEncrypt
        val contentLength = if (encrypt) -1 else calculateSize(context.contentResolver, uri)
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentLength() = contentLength

            override fun contentType() = getMimeType(finalFileName).toMediaTypeOrNull()

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val `in` = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Could not read $uri")
                val source = maybeEncrypt(`in`, maybeEncrypt).source()
                try {
                    sink.writeAll(source)
                } finally {
                    source.closeQuietly()
                }
            }
        }
        try {
            webDavClient.upload(finalFileName, requestBody, folder)
        } catch (e: HttpException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun storeBackup(uri: Uri, fileName: String) {
        webDavClient.mkCol(BACKUP_FOLDER_NAME)
        saveUriToFolder(fileName, uri, BACKUP_FOLDER_NAME, false)
    }

    override val storedBackups: List<String>
        get() = try {
            webDavClient.getFolderMembers(BACKUP_FOLDER_NAME)
                .map { obj: DavResource -> obj.fileName() }
        } catch (e: IOException) {
            ArrayList()
        }

    @Throws(IOException::class)
    override fun saveFileContents(
        toAccountDir: Boolean,
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        val base = if(toAccountDir) webDavClient.getCollection(accountUuid) else webDavClient.base
        val parent: LockableDavResource
        if (folder != null) {
            webDavClient.mkCol(folder, base)
            parent = webDavClient.getCollection(folder, accountUuid)
            if (!parent.exists()) {
                throw IOException("Cannot make folder")
            }
        } else {
            parent = base
        }
        saveFileContents(fileName, fileContents, mimeType, maybeEncrypt, parent)
    }

    private fun transform(e: HttpException): IOException? {
        return if (e.cause is IOException) e.cause as IOException? else IOException(e)
    }

    @Throws(IOException::class)
    private fun saveFileContents(
        fileName: String, fileContents: String, mimeType: String,
        maybeEncrypt: Boolean, parent: LockableDavResource
    ) {
        val encrypt = isEncrypted && maybeEncrypt
        val mediaType: MediaType? = "$mimeType; charset=utf-8".toMediaTypeOrNull()
        val requestBody: RequestBody = if (encrypt) object : RequestBody() {
            override fun contentType(): MediaType? {
                return mediaType
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val source = toInputStream(fileContents, true).source()
                try {
                    sink.writeAll(source)
                } finally {
                    source.closeQuietly()
                }
            }
        } else fileContents.toRequestBody(mediaType)
        try {
            webDavClient.upload(fileName, requestBody, parent)
        } catch (e: HttpException) {
            throw transform(e)!!
        }
    }

    private fun getLastPathSegment(httpUrl: HttpUrl): String {
        val segments = httpUrl.pathSegments
        return segments[segments.size - 1]
    }

    @get:Throws(IOException::class)
    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = webDavClient.getFolderMembers()
            .asSequence()
            .filter { davResource: DavResource? -> LockableDavResource.isCollection(davResource) }
            .filter { davResource: DavResource -> getLastPathSegment(davResource.location) != BACKUP_FOLDER_NAME }
            .map { davResource: DavResource ->
                webDavClient.getResource(
                    davResource.location,
                    accountMetadataFilename
                )
            }
            .filter { obj: LockableDavResource -> obj.exists() }
            .map { lockableDavResource: LockableDavResource ->
                getAccountMetaDataFromDavResource(
                    lockableDavResource
                )
            }
            .toList()

    private fun getAccountMetaDataFromDavResource(lockableDavResource: LockableDavResource): Result<AccountMetaData> =
        getAccountMetaDataFromInputStream(lockableDavResource[mimeTypeForData].byteStream())

    companion object {
        const val KEY_WEB_DAV_CERTIFICATE = "webDavCertificate"
        const val KEY_WEB_DAV_FALLBACK_TO_CLASS1 = "fallbackToClass1"
        const val KEY_ALLOW_UNVERIFIED = "allow_unverified"
    }

    init {
        val url = accountManager.getSyncProviderUrl(account)
        val userName =
            accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_USERNAME)
        val password = accountManager.getPassword(account)
        fallbackToClass1 =
            accountManager.getUserData(account, KEY_WEB_DAV_FALLBACK_TO_CLASS1) != null
        val allowUnverified = "true" == accountManager.getUserData(account, KEY_ALLOW_UNVERIFIED)
        var certificate: X509Certificate? = null
        if (accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE) != null) {
            certificate = try {
                fromString(accountManager.getUserData(account, KEY_WEB_DAV_CERTIFICATE))
            } catch (e: CertificateException) {
                throw SyncParseException(e)
            }
        }
        webDavClient = try {
            WebDavClient(url, userName, password, certificate, allowUnverified)
        } catch (e: InvalidCertificateException) {
            throw SyncParseException(e)
        }
    }
}