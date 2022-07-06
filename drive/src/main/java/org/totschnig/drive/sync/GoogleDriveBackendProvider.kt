package org.totschnig.drive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.model.File
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SequenceNumber
import org.totschnig.myexpenses.sync.SyncBackendProvider.AuthException
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.StreamReader
import org.totschnig.myexpenses.util.io.getMimeType
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class GoogleDriveBackendProvider internal constructor(
    context: Context,
    account: Account,
    accountManager: AccountManager
) : AbstractSyncBackendProvider(context) {
    private val folderId: String = accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL) ?: throw SyncParseException("Drive folder not set")
    private lateinit var baseFolder: File
    private lateinit var accountFolder: File
    private var driveServiceHelper: DriveServiceHelper = try {
        DriveServiceHelper(
            context,
            accountManager.getUserData(account, KEY_GOOGLE_ACCOUNT_EMAIL)
        )
    } catch (e: Exception) {
        throw SyncParseException(e)
    }
    override val sharedPreferencesName: String
        get() = "google_drive_backend"

    @Throws(IOException::class)
    override fun readEncryptionToken(): String? {
        return try {
            StreamReader(getInputStream(baseFolder, ENCRYPTION_TOKEN_FILE_NAME)).read()
        } catch (e: FileNotFoundException) {
            null
        }
    }

    @Throws(Exception::class)
    override fun setUp(
        accountManager: AccountManager,
        account: Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        try {
            super.setUp(accountManager, account, encryptionPassword, create)
        } catch (e: UserRecoverableAuthIOException) {
            throw AuthException(e, e.intent)
        }
    }

    @get:Throws(IOException::class)
    override val isEmpty: Boolean
        get() = driveServiceHelper.listChildren(baseFolder).isEmpty()

    @Throws(IOException::class)
    override fun getInputStreamForPicture(relativeUri: String): InputStream {
        return getInputStream(accountFolder, relativeUri)
    }

    @Throws(IOException::class)
    override fun getInputStreamForBackup(backupFile: String): InputStream {
        val backupFolder = getBackupFolder(false)
        return backupFolder?.let { getInputStream(it, backupFile) }
            ?: throw IOException("No backup folder found")
    }

    @Throws(IOException::class)
    private fun getInputStream(folder: File, title: String): InputStream {
        return driveServiceHelper.downloadFile(folder, title)
    }

    @Throws(IOException::class)
    override fun saveUriToAccountDir(fileName: String, uri: Uri) {
        saveUriToFolder(fileName, uri, accountFolder, true)
    }

    @Throws(IOException::class)
    private fun saveUriToFolder(
        fileName: String,
        uri: Uri,
        driveFolder: File?,
        maybeEncrypt: Boolean
    ) {
        (context.contentResolver.openInputStream(uri) ?: throw IOException("Could not read $uri")).use {
            saveInputStream(
                fileName,
                if (maybeEncrypt) maybeEncrypt(it) else it,
                getMimeType(fileName),
                driveFolder
            )
        }
    }

    @Throws(IOException::class)
    override fun storeBackup(uri: Uri, fileName: String) {
        saveUriToFolder(fileName, uri, getBackupFolder(true), false)
    }

    @get:Throws(IOException::class)
    override val storedBackups: List<String>
        get() = getBackupFolder(false)?.let {
            driveServiceHelper.listChildren(it)
                .map { obj: File -> obj.name }
        } ?: emptyList()

    @Throws(IOException::class)
    override fun getLastSequence(start: SequenceNumber): SequenceNumber {
        val resourceComparator = java.util.Comparator { o1: File, o2: File ->
            Utils.compare(
                getSequenceFromFileName(o1.name),
                getSequenceFromFileName(o2.name)
            )
        }
        val lastShardOptional = driveServiceHelper.listFolders(accountFolder)
            .filter { file: File -> isAtLeastShardDir(start.shard, file.name) }
            .maxWithOrNull(resourceComparator)
        val lastShard: List<File>
        val lastShardInt: Int
        val reference: Int
        if (lastShardOptional != null) {
            lastShard = driveServiceHelper.listChildren(lastShardOptional)
            lastShardInt = getSequenceFromFileName(lastShardOptional.name)
            reference = if (lastShardInt == start.shard) start.number else 0
        } else {
            if (start.shard > 0) return start
            lastShard = driveServiceHelper.listChildren(accountFolder)
            lastShardInt = 0
            reference = start.number
        }
        return lastShard
            .filter { metadata: File -> isNewerJsonFile(reference, metadata.name) }
            .maxWithOrNull(resourceComparator)?.let {
                SequenceNumber(
                    lastShardInt,
                    getSequenceFromFileName(it.name)
                )
            } ?: start
    }

    @Throws(IOException::class)
    override fun saveFileContentsToBase(
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        saveFileContents(baseFolder, fileName, fileContents, mimeType, maybeEncrypt)
    }

    @Throws(IOException::class)
    override fun saveFileContentsToAccountDir(
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        var driveFolder: File?
        if (folder == null) {
            driveFolder = accountFolder
        } else {
            driveFolder = getSubFolder(folder)
            if (driveFolder == null) {
                driveFolder = driveServiceHelper.createFolder(accountFolder.id, folder, null)
            }
        }
        saveFileContents(driveFolder, fileName, fileContents, mimeType, maybeEncrypt)
    }

    @Throws(IOException::class)
    private fun saveFileContents(
        driveFolder: File?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        toInputStream(fileContents, maybeEncrypt).use {
            saveInputStream(fileName, it, mimeType, driveFolder)
        }
    }

    @Suppress("SameParameterValue")
    override val existingLockToken: String?
        get() {
            val appProperties = accountFolder.appProperties
            return appProperties?.get(LOCK_TOKEN_KEY)
        }

    @Throws(IOException::class)
    override fun writeLockToken(lockToken: String) {
        driveServiceHelper.setMetadataProperty(accountFolder.id, LOCK_TOKEN_KEY, lockToken)
    }

    @Throws(IOException::class)
    private fun saveInputStream(
        fileName: String,
        contents: InputStream,
        mimeType: String,
        driveFolder: File?
    ) {
        val file = driveServiceHelper.createFile(
            driveFolder!!.id, fileName, mimeType, null
        )
        try {
            driveServiceHelper.saveFile(file.id, mimeType, contents)
        } catch (e: Exception) {
            try {
                driveServiceHelper.delete(file.id)
            } catch (e: Exception) {
                log().w(e)
            }
            throw e
        }
    }

    @Throws(IOException::class)
    override fun withAccount(account: org.totschnig.myexpenses.model.Account) {
        setAccountUuid(account)
        writeAccount(account, false)
    }

    @Throws(IOException::class)
    override fun writeAccount(account: org.totschnig.myexpenses.model.Account, update: Boolean) {
        val existingAccountFolder = getExistingAccountFolder(account.uuid!!)
        if (existingAccountFolder == null) {
            accountFolder = driveServiceHelper.createFolder(baseFolder.id, accountUuid!!, null)
            createWarningFile()
        } else {
            accountFolder = existingAccountFolder
        }
        if (update || existingAccountFolder == null) {
            saveFileContentsToAccountDir(
                null,
                accountMetadataFilename,
                buildMetadata(account),
                mimeTypeForData,
                true
            )
        }
    }

    override fun readAccountMetaData(): Result<AccountMetaData> {
        return getAccountMetaDataFromDriveMetadata(accountFolder)
    }

    @Throws(IOException::class)
    override fun resetAccountData(uuid: String) {
        val existingAccountFolder = getExistingAccountFolder(uuid)
        if (existingAccountFolder != null) {
            driveServiceHelper.delete(existingAccountFolder.id)
        }
    }

    @Throws(IOException::class)
    override fun getChangeSetSince(
        sequenceNumber: SequenceNumber,
        context: Context
    ): ChangeSet? = merge(buildList {
        var nextShard = sequenceNumber.shard
        var startNumber = sequenceNumber.number
        while (true) {
            val nextShardFolder = if (nextShard == 0) accountFolder else getSubFolder("_$nextShard")
            if (nextShardFolder != null) {
                val fileList = driveServiceHelper.listChildren(nextShardFolder).sortedBy { getSequenceFromFileName(it.name) }
                log().i("Getting data from shard %d", nextShard)
                for (metadata in fileList) {
                    if (isNewerJsonFile(startNumber, metadata.name)) {
                        if ((metadata.getSize() ?: 0) > 0) {
                            log().i("Getting data from file %s", metadata.name)
                            add(getChangeSetFromMetadata(nextShard, metadata))
                        } else {
                            log().i("Found 0-size file %s", metadata.name)
                        }
                    }
                }
                nextShard++
                startNumber = 0
            } else {
                break
            }
        }
    })

    @Throws(IOException::class)
    private fun getSubFolder(shard: String): File? {
        return driveServiceHelper.getFileByNameAndParent(accountFolder, shard)
    }

    @Throws(IOException::class)
    private fun getChangeSetFromMetadata(shard: Int, metadata: File): ChangeSet {
        return getChangeSetFromInputStream(
            SequenceNumber(shard, getSequenceFromFileName(metadata.name)),
            driveServiceHelper.read(metadata.id)
        )
    }

    @Throws(IOException::class)
    override fun unlock() {
        driveServiceHelper.setMetadataProperty(accountFolder.id, LOCK_TOKEN_KEY, null)
    }

    @get:Throws(IOException::class)
    override val remoteAccountList: List<Result<AccountMetaData>>
        get() {
            val fileList = driveServiceHelper.listChildren(baseFolder)
            return fileList
                .filter { file: File? ->
                    driveServiceHelper.isFolder(
                        file!!
                    )
                }
                .filter { metadata: File -> metadata.name != BACKUP_FOLDER_NAME }
                .map { metadata: File? -> getAccountMetaDataFromDriveMetadata(metadata) }
        }

    private fun getAccountMetaDataFromDriveMetadata(metadata: File?): Result<AccountMetaData> {
        val accountMetadata: File? = try {
            driveServiceHelper.getFileByNameAndParent(metadata!!, accountMetadataFilename)
        } catch (e: IOException) {
            return Result.failure(e)
        }
        if (accountMetadata != null) {
            try {
                driveServiceHelper.read(accountMetadata.id)
                    .use { inputStream -> return getAccountMetaDataFromInputStream(inputStream) }
            } catch (e: IOException) {
                return Result.failure(e)
            }
        }

        //legacy
        val appProperties = metadata.appProperties
            ?: return Result.failure(Exception("appProperties are null"))
        val uuid = appProperties[ACCOUNT_METADATA_UUID_KEY]
        if (uuid == null) {
            Timber.d("UUID property not set")
            return Result.failure(Exception("UUID property not set"))
        }
        return kotlin.runCatching {
            AccountMetaData.builder()
                .setType(
                    getPropertyWithDefault(
                        appProperties,
                        ACCOUNT_METADATA_TYPE_KEY,
                        AccountType.CASH.name
                    )
                )
                .setOpeningBalance(
                    getPropertyWithDefault(
                        appProperties,
                        ACCOUNT_METADATA_OPENING_BALANCE_KEY,
                        0L
                    )
                )
                .setDescription(
                    getPropertyWithDefault(
                        appProperties,
                        ACCOUNT_METADATA_DESCRIPTION_KEY,
                        ""
                    )
                )
                .setColor(
                    getPropertyWithDefault(
                        appProperties,
                        ACCOUNT_METADATA_COLOR_KEY,
                        org.totschnig.myexpenses.model.Account.DEFAULT_COLOR
                    )
                )
                .setCurrency(
                    getPropertyWithDefault(
                        appProperties, ACCOUNT_METADATA_CURRENCY_KEY,
                        Utils.getHomeCurrency().code
                    )
                )
                .setUuid(uuid)
                .setLabel(metadata.name).build()
        }
    }

    private fun getPropertyWithDefault(
        metadata: Map<String, String>,
        key: String,
        defaultValue: String
    ): String {
        val result = metadata[key]
        return result ?: defaultValue
    }

    @Suppress("SameParameterValue", "SameParameterValue")
    private fun getPropertyWithDefault(
        metadata: Map<String, String>,
        key: String,
        defaultValue: Long
    ): Long {
        val result = metadata[key]
        return result?.toLong() ?: defaultValue
    }

    @Suppress("SameParameterValue", "SameParameterValue")
    private fun getPropertyWithDefault(
        metadata: Map<String, String>,
        key: String,
        defaultValue: Int
    ): Int {
        val result = metadata[key]
        return result?.toInt() ?: defaultValue
    }

    @Suppress("SameParameterValue")
    private fun getPropertyWithDefault(
        metadata: Map<String, String>,
        key: String,
        defaultValue: Boolean
    ): Boolean {
        val result = metadata[key]
        return if (result != null) java.lang.Boolean.parseBoolean(result) else defaultValue
    }

    @Throws(IOException::class)
    private fun getBackupFolder(require: Boolean): File? {
        val file = driveServiceHelper.getFileByNameAndParent(
            baseFolder, BACKUP_FOLDER_NAME
        )
        if (file != null && file.appProperties != null && getPropertyWithDefault(
                file.appProperties,
                IS_BACKUP_FOLDER,
                false
            )
        ) {
            return file
        }
        if (require) {
            val properties: MutableMap<String, String> = HashMap()
            properties[IS_BACKUP_FOLDER] = "true"
            return driveServiceHelper.createFolder(
                baseFolder.id,
                BACKUP_FOLDER_NAME,
                properties
            )
        }
        return null
    }

    @Throws(IOException::class)
    private fun getExistingAccountFolder(uuid: String): File? {
        return driveServiceHelper.getFileByNameAndParent(baseFolder, uuid)
    }

    @Throws(IOException::class)
    private fun requireBaseFolder() {
        baseFolder = driveServiceHelper.getFile(folderId)
    }

    companion object {
        const val KEY_GOOGLE_ACCOUNT_EMAIL = "googleAccountEmail"
        private const val ACCOUNT_METADATA_CURRENCY_KEY = "accountMetadataCurrency"
        private const val ACCOUNT_METADATA_COLOR_KEY = "accountMetadataColor"
        private const val ACCOUNT_METADATA_UUID_KEY = "accountMetadataUuid"
        private const val ACCOUNT_METADATA_OPENING_BALANCE_KEY = "accountMetadataOpeningBalance"
        private const val ACCOUNT_METADATA_DESCRIPTION_KEY = "accountMetadataDescription"
        private const val ACCOUNT_METADATA_TYPE_KEY = "accountMetadataType"
        private const val LOCK_TOKEN_KEY = KEY_LOCK_TOKEN
        private const val IS_BACKUP_FOLDER = "isBackupFolder"
        const val IS_SYNC_FOLDER = "isSyncFolder"
    }

    init {
        requireBaseFolder()
    }
}