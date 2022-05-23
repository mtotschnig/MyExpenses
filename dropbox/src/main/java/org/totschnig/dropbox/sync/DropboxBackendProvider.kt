package org.totschnig.dropbox.sync

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.core.util.Pair
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.InvalidAccessTokenException
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.WriteMode
import org.totschnig.dropbox.activity.ACTION_RE_AUTHENTICATE
import org.totschnig.dropbox.activity.DropboxSetup
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.*
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.Preconditions
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.io.StreamReader
import java.io.IOException
import java.io.InputStream
import java.util.*

class DropboxBackendProvider internal constructor(context: Context, folderName: String) :
    AbstractSyncBackendProvider(context) {
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

    @Throws(IOException::class)
    override fun readEncryptionToken(): String? {
        val resourcePath = "$basePath/$ENCRYPTION_TOKEN_FILE_NAME"
        return if (!exists(resourcePath)) {
            null
        } else StreamReader(
            getInputStream(
                resourcePath
            )
        ).read()
    }

    @Throws(IOException::class)
    override fun withAccount(account: Account) {
        setAccountUuid(account)
        val accountPath = accountPath
        requireFolder(accountPath)
        val metadataPath = getResourcePath(accountMetadataFilename)
        if (!exists(metadataPath)) {
            saveFileContentsToAccountDir(
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
            saveFileContentsToAccountDir(
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

    @Throws(IOException::class)
    private fun exists(path: String) = tryWithWrappedException {
        try {
            mDbxClient.files().getMetadata(path)
            true
        } catch (e: GetMetadataErrorException) {
            if (e.errorValue.isPath && e.errorValue.pathValue.isNotFound) {
                false
            } else {
                throw e
            }
        }
    }

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

    override val existingLockToken: String?
        get() = lockFilePath.takeIf { exists(it) }?.let {
            StreamReader(getInputStream(lockFilePath)).read()
        }

    @Throws(IOException::class)
    private fun getInputStream(resourcePath: String) = tryWithWrappedException {
        mDbxClient.files().download(resourcePath).inputStream
    }

    @Throws(IOException::class)
    override fun writeLockToken(lockToken: String) {
        saveInputStream(lockFilePath, toInputStream(lockToken, false))
    }

    @Throws(IOException::class)
    override fun unlock() {
        tryWithWrappedException {
            mDbxClient.files().deleteV2(lockFilePath)
        }
    }

    private val lockFilePath: String
        get() = getResourcePath(LOCK_FILE)

    @Throws(IOException::class)
    override fun getChangeSetSince(
        sequenceNumber: SequenceNumber,
        context: Context
    ): ChangeSet? {
        val changeSetList: MutableList<ChangeSet> = ArrayList()
        for (integerMetadataPair in filterMetadata(sequenceNumber)) {
            changeSetList.add(getChangeSetFromMetadata(integerMetadataPair))
        }
        return merge(changeSetList)
    }

    @Throws(IOException::class)
    private fun getChangeSetFromMetadata(metadata: Pair<Int, Metadata>): ChangeSet {
        return getChangeSetFromInputStream(
            SequenceNumber(metadata.first, getSequenceFromFileName(metadata.second.name)),
            getInputStream(metadata.second.pathLower)
        )
    }

    @Throws(IOException::class)
    private fun filterMetadata(sequenceNumber: SequenceNumber): List<Pair<Int, Metadata>> =
        tryWithWrappedException {
            buildList {
                var nextShard = sequenceNumber.shard
                var startNumber = sequenceNumber.number
                while (true) {
                    val nextShardPath =
                        if (nextShard == 0) accountPath else "$accountPath/_$nextShard"
                    if (exists(nextShardPath)) {
                        addAll(
                            mDbxClient.files().listFolder(nextShardPath).entries
                                .sortedBy { getSequenceFromFileName(it.name) }
                                .filter { metadata: Metadata ->
                                    isNewerJsonFile(
                                        startNumber,
                                        metadata.name
                                    )
                                }
                                .map { metadata: Metadata -> Pair.create(nextShard, metadata) }
                        )
                        nextShard++
                        startNumber = 0
                    } else {
                        break
                    }
                }
            }
        }

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
        val `in` = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not read $uri")
        val finalFileName = getLastFileNamePart(fileName)
        saveInputStream("$folder/$finalFileName", if (maybeEncrypt) maybeEncrypt(`in`) else `in`)
    }

    @Throws(IOException::class)
    override fun getLastSequence(start: SequenceNumber): SequenceNumber {
        val resourceComparator = Comparator { o1: Metadata, o2: Metadata ->
            Utils.compare(
                getSequenceFromFileName(o1.name),
                getSequenceFromFileName(o2.name)
            )
        }
        return tryWithWrappedException {
            val accountPath = accountPath
            val mainEntries = mDbxClient.files().listFolder(accountPath).entries
            val lastShardOptional = mainEntries
                .filter { metadata: Metadata ->
                    metadata is FolderMetadata && isAtLeastShardDir(
                        start.shard,
                        metadata.getName()
                    )
                }
                .maxWithOrNull(resourceComparator)
            val lastShard: List<Metadata>
            val lastShardInt: Int
            val reference: Int
            if (lastShardOptional != null) {
                val lastShardName = lastShardOptional.name
                lastShard = mDbxClient.files().listFolder("$accountPath/$lastShardName").entries
                lastShardInt = getSequenceFromFileName(lastShardName)
                reference = if (lastShardInt == start.shard) start.number else 0
            } else {
                if (start.shard > 0) return@tryWithWrappedException start
                lastShard = mainEntries
                lastShardInt = 0
                reference = start.number
            }
            lastShard
                .filter { metadata: Metadata -> isNewerJsonFile(reference, metadata.name) }
                .maxWithOrNull(resourceComparator)
                ?.let { metadata: Metadata ->
                    SequenceNumber(
                        lastShardInt,
                        getSequenceFromFileName(metadata.name)
                    )
                } ?: start
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
    override fun saveFileContentsToAccountDir(
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        val path: String
        val accountPath = accountPath
        if (folder == null) {
            path = accountPath
        } else {
            path = "$accountPath/$folder"
            requireFolder(path)
        }
        saveInputStream("$path/$fileName", toInputStream(fileContents, maybeEncrypt))
    }

    @Throws(IOException::class)
    override fun saveFileContentsToBase(
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        saveInputStream("$basePath/$fileName", toInputStream(fileContents, maybeEncrypt))
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

    override val sharedPreferencesName = "webdav_backend"


    private fun reAuthenticationIntent() = Intent(context, DropboxSetup::class.java).apply {
        action = ACTION_RE_AUTHENTICATE
        putExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName)
    }

    companion object {
        private const val LOCK_FILE = ".lock"
    }

}