package org.totschnig.dropbox.sync

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.InvalidAccessTokenException
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.WriteMode
import org.acra.util.StreamReader
import org.totschnig.dropbox.activity.ACTION_RE_AUTHENTICATE
import org.totschnig.dropbox.activity.DropboxSetup
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.*
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.Preconditions
import java.io.IOException
import java.io.InputStream
import java.util.*

class DropboxBackendProvider internal constructor(context: Context, folderName: String) :
    AbstractSyncBackendProvider<Metadata>(context) {
    private lateinit var mDbxClient: DbxClientV2
    private val basePath: String = "/$folderName"
    private lateinit var accountName: String

    override fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        this.accountName = account.name
        setupClient(accountManager, account)
        super.setUp(accountManager, account, encryptionPassword, create)
    }

    override val isEmpty: Boolean
        get() = tryWithWrappedException {
            mDbxClient.files().listFolder(basePath).entries.isEmpty()
        }

    private fun setupClient(accountManager: AccountManager, account: android.accounts.Account) {
        val userLocale = Locale.getDefault().toString()
        val requestConfig =
            DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID).withUserLocale(userLocale)
                .build()
        val dbxCredential = accountManager.getUserData(account, KEY_DBX_CREDENTIAL)
        mDbxClient = if (dbxCredential != null) {
            SyncAdapter.log().i("Authenticating with DbxCredential")
            DbxClientV2(requestConfig, DbxCredential.Reader.readFully(dbxCredential))

        } else {
            val authToken = accountManager.peekAuthToken(
                account,
                GenericAccountService.AUTH_TOKEN_TYPE
            )
                ?: throw SyncBackendProvider.AuthException(
                    NullPointerException("authToken is null"),
                    reAuthenticationIntent()
                )
            SyncAdapter.log().i("Authenticating with legacy access token")
            DbxClientV2(requestConfig, authToken)
        }
    }

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ) = "${if (fromAccountDir) accountPath else basePath}/$fileName"
        .takeIf { exists(it) }?.let { file ->
            getInputStream(file)?.use { StreamReader(maybeDecrypt(it, maybeDecrypt)).read() }
        }

    @Throws(IOException::class)
    override fun withAccount(account: Account) {
        setAccountUuid(account)
        val accountPath = accountPath
        requireFolder(accountPath)
        val metadataPath = getResourcePath(accountMetadataFilename)
        if (!exists(metadataPath)) {
            saveFileContents(
                true,
                null,
                accountMetadataFilename,
                buildMetadata(account),
                mimeTypeForData,
                true
            )
            createWarningFile()
        }
    }

    @Throws(IOException::class)
    override fun writeAccount(account: Account, update: Boolean) {
        val metadataPath = getResourcePath(accountMetadataFilename)
        if (update || !exists(metadataPath)) {
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
        return getAccountMetaDataFromPath(getResourcePath(accountMetadataFilename))
    }

    private fun metadata(path: String) = tryWithWrappedException {
        try {
            mDbxClient.files().getMetadata(path)
        } catch (e: GetMetadataErrorException) {
            if (e.errorValue.isPath && e.errorValue.pathValue.isNotFound) {
                null
            } else {
                throw e
            }
        }
    }

    @Throws(IOException::class)
    private fun exists(path: String) = metadata(path) != null

    @Throws(IOException::class)
    private fun requireFolder(path: String) {
        if (!exists(path)) {
            tryWithWrappedException {
                mDbxClient.files().createFolderV2(path)
            }
        }
    }

    private val accountPath: String
        get() {
            Preconditions.checkArgument(!TextUtils.isEmpty(basePath))
            Preconditions.checkArgument(!TextUtils.isEmpty(accountUuid))
            return "$basePath/$accountUuid"
        }
    private val backupPath: String
        get() = "$basePath/$BACKUP_FOLDER_NAME"

    private fun getResourcePath(resource: String): String = "$accountPath/$resource"

    @Throws(IOException::class)
    override fun resetAccountData(uuid: String) {
        Preconditions.checkArgument(!TextUtils.isEmpty(basePath))
        Preconditions.checkArgument(!TextUtils.isEmpty(uuid))
        tryWithWrappedException {
            mDbxClient.files().deleteV2("$basePath/$uuid")
        }
    }

    @Throws(IOException::class)
    private fun getInputStream(resourcePath: String) = tryWithWrappedException {
        mDbxClient.files().download(resourcePath).inputStream
    }

    override fun deleteLockTokenFile() {
        tryWithWrappedException {
            mDbxClient.files().deleteV2(lockFilePath)
        }
    }

    private val lockFilePath: String
        get() = getResourcePath(LOCK_FILE)

    override fun getChangeSetFromResource(shardNumber: Int, resource: Metadata): ChangeSet {
        return getChangeSetFromInputStream(
            SequenceNumber(shardNumber, getSequenceFromFileName(resource.name)),
            getInputStream(resource.pathLower)
        )
    }

    override fun collectionForShard(shardNumber: Int) = metadata(
        if (shardNumber == 0) accountPath else "$accountPath/${folderForShard(shardNumber)}"
    )

    override fun childrenForCollection(folder: Metadata?): List<Metadata> =
        mDbxClient.files().listFolder(folder?.pathLower ?: accountPath).entries

    override fun nameForResource(resource: Metadata): String = resource.name

    override fun isCollection(resource: Metadata) = resource is FolderMetadata

    @Throws(IOException::class)
    override fun getInputStreamForPicture(relativeUri: String): InputStream {
        return getInputStream(getResourcePath(relativeUri))
    }

    @Throws(IOException::class)
    override fun getInputStreamForBackup(backupFile: String): InputStream {
        return getInputStream("$backupPath/$backupFile")
    }

    @Throws(IOException::class)
    override fun storeBackup(uri: Uri, fileName: String) {
        val backupPath = backupPath
        requireFolder(backupPath)
        saveUriToFolder(fileName, uri, backupPath, false)
    }

    @Throws(IOException::class)
    override fun saveUriToAccountDir(fileName: String, uri: Uri) {
        saveUriToFolder(fileName, uri, accountPath, true)
    }

    @Throws(IOException::class)
    private fun saveUriToFolder(fileName: String, uri: Uri, folder: String, maybeEncrypt: Boolean) {
        saveInputStream(
            "$folder/${getLastFileNamePart(fileName)}",
            maybeEncrypt(
                context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Could not read $uri"),
                maybeEncrypt
            )
        )
    }

    override fun getLastSequence(start: SequenceNumber): SequenceNumber {
        return tryWithWrappedException {
            super.getLastSequence(start)
        }
    }

    private fun <T> tryWithWrappedException(block: () -> T): T = try {
        block()
    } catch (e: DbxException) {
        throw (if (e is InvalidAccessTokenException) SyncBackendProvider.AuthException(
            e,
            reAuthenticationIntent()
        ) else IOException(e))
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
        val base = if (toAccountDir) accountPath else basePath
        val path = if (folder == null) {
            base
        } else {
            "$base/$folder".also {
                requireFolder(it)
            }
        }
        saveInputStream("$path/$fileName", toInputStream(fileContents, maybeEncrypt))
    }

    @Throws(IOException::class)
    private fun saveInputStream(path: String, contents: InputStream) {
        tryWithWrappedException {
            mDbxClient.files().uploadBuilder(path)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(contents)
        }
    }

    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = tryWithWrappedException {
            mDbxClient.files().listFolder(basePath).entries
                .asSequence()
                .filterIsInstance<FolderMetadata>()
                .filter { metadata: Metadata -> metadata.name != BACKUP_FOLDER_NAME }
                .map { metadata: Metadata -> basePath + "/" + metadata.name + "/" + accountMetadataFilename }
                .filter { accountMetadataPath: String? ->
                    try {
                        mDbxClient.files().getMetadata(accountMetadataPath)
                        return@filter true
                    } catch (e: DbxException) {
                        return@filter false
                    }
                }
                .map { path: String -> getAccountMetaDataFromPath(path) }
                .toList()
        }

    private fun getAccountMetaDataFromPath(path: String): Result<AccountMetaData> =
        getAccountMetaDataFromInputStream(getInputStream(path))

    override val storedBackups: List<String>
        get() = try {
            mDbxClient.files().listFolder(
                backupPath
            ).entries
                .map { obj: Metadata -> obj.name }
        } catch (ignored: DbxException) {
            emptyList()
        }

    override val sharedPreferencesName = "dropbox"

    private fun reAuthenticationIntent() = Intent(context, DropboxSetup::class.java).apply {
        action = ACTION_RE_AUTHENTICATE
        putExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName)
    }
}