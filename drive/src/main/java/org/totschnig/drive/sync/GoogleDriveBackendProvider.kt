package org.totschnig.drive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.File
import org.acra.util.StreamReader
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.sync.AbstractSyncBackendProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProvider.AuthException
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.io.getMimeType
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class GoogleDriveBackendProvider internal constructor(
    context: Context,
    account: Account,
    accountManager: AccountManager
) : AbstractSyncBackendProvider<File>(context) {
    private val folderId: String =
        accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL)
            ?: throw SyncParseException("Drive folder not set")
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
    override val sharedPreferencesName = "google_drive"

    override val accountRes: File
        get() = accountFolder

    override fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean
    ): String? {
        return try {
            StreamReader(
                maybeDecrypt(
                    getInputStream(
                        if (fromAccountDir) accountFolder else baseFolder,
                        fileName
                    ),
                    maybeDecrypt
                )
            ).read()
        } catch (e: FileNotFoundException) {
            null
        }
    }

    @Throws(Exception::class)
    override suspend fun setUp(
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

    override fun getInputStream(resource: File) = driveServiceHelper.read(resource.id)

    @Throws(IOException::class)
    private fun getInputStream(folder: File, title: String): InputStream {
        return driveServiceHelper.downloadFile(folder, title)
    }

    @Throws(IOException::class)
    override fun saveUriToCollection(
        fileName: String,
        uri: Uri,
        collection: File,
        maybeEncrypt: Boolean
    ) {
        (context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not read $uri")).use {
            saveInputStream(
                fileName,
                if (maybeEncrypt) maybeEncrypt(it) else it,
                getMimeType(fileName),
                collection
            )
        }
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
        val base = if (toAccountDir) accountFolder else baseFolder
        val driveFolder = if (folder == null) base else  {
            driveServiceHelper.getFileByNameAndParent(base, folder) ?: driveServiceHelper.createFolder(
                base.id,
                folder,
                null
            )
        }
        saveFileContents(driveFolder, fileName, fileContents, mimeType, maybeEncrypt)
    }

    @Throws(IOException::class)
    private fun saveFileContents(
        driveFolder: File,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    ) {
        toInputStream(fileContents, maybeEncrypt).use {
            saveInputStream(fileName, it, mimeType, driveFolder)
        }
    }

    override fun getLockToken() = accountFolder.appProperties?.get(LOCK_TOKEN_KEY)

    override fun setLockToken(lockToken: String) {
        driveServiceHelper.setMetadataProperty(accountFolder.id, LOCK_TOKEN_KEY, lockToken)
    }

    override fun deleteLockTokenFile() {
        driveServiceHelper.setMetadataProperty(accountFolder.id, LOCK_TOKEN_KEY, null)
    }

    @Throws(IOException::class)
    private fun saveInputStream(
        fileName: String,
        contents: InputStream,
        mimeType: String,
        driveFolder: File
    ) {
        val file = driveServiceHelper.createFile(
            driveFolder.id, fileName, mimeType, null
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
    override fun withAccount(account: org.totschnig.myexpenses.model2.Account) {
        super.withAccount(account)
        writeAccount(account, false)
    }

    @Throws(IOException::class)
    override fun writeAccount(account: org.totschnig.myexpenses.model2.Account, update: Boolean) {
        val existingAccountFolder = getExistingAccountFolder(accountUuid)
        if (existingAccountFolder == null) {
            accountFolder = driveServiceHelper.createFolder(baseFolder.id, accountUuid, null)
            createWarningFile()
        } else {
            accountFolder = existingAccountFolder
        }
        if (update || existingAccountFolder == null) {
            saveFileContents(
                true,
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

    override fun getResInAccountDir(resourceName: String) =
        driveServiceHelper.getFileByNameAndParent(accountFolder, resourceName)

    override fun getCollection(collectionName: String, require: Boolean): File? {
        val file = driveServiceHelper.getFileByNameAndParent(
            baseFolder, collectionName
        )
        val key = collectionPropertyKey(collectionName)
        return when {
            file != null && file.appProperties != null &&
                    getPropertyWithDefault(file.appProperties, key, false) -> file

            require -> {
                val properties: MutableMap<String, String> = HashMap()
                properties[key] = "true"
                driveServiceHelper.createFolder(
                    baseFolder.id,
                    collectionName,
                    properties
                )
            }

            else -> null
        }
    }

    override fun childrenForCollection(folder: File?) =
        driveServiceHelper.listChildren(folder ?: accountFolder).filter {
            @Suppress("UsePropertyAccessSyntax")
            it.getSize()?.compareTo(0) != 0
        }

    override fun nameForResource(resource: File): String? = resource.name

    override fun isCollection(resource: File) = driveServiceHelper.isFolder(resource)

    @get:Throws(IOException::class)
    override val remoteAccountList: List<Result<AccountMetaData>>
        get() = driveServiceHelper.listChildren(baseFolder)
            .filter(driveServiceHelper::isFolder)
            .filter { verifyRemoteAccountFolderName(it.name) }
            .map(::getAccountMetaDataFromDriveMetadata)

    private fun getAccountMetaDataFromDriveMetadata(metadata: File): Result<AccountMetaData> {
        val accountMetadata: File? = try {
            driveServiceHelper.getFileByNameAndParent(metadata, accountMetadataFilename)
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
                        org.totschnig.myexpenses.model2.Account.DEFAULT_COLOR
                    )
                )
                .setCurrency(
                    getPropertyWithDefault(
                        appProperties, ACCOUNT_METADATA_CURRENCY_KEY,
                        homeCurrency
                    )
                )
                .setUuid(uuid)
                .setLabel(metadata.name).build()
        }
    }

    private val homeCurrency: String
        get() = context.injector.currencyContext().homeCurrencyString

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
    private fun getExistingAccountFolder(uuid: String): File? {
        return driveServiceHelper.getFileByNameAndParent(baseFolder, uuid)
    }

    @Throws(IOException::class)
    private fun requireBaseFolder() {
        try {
            baseFolder = driveServiceHelper.getFile(folderId)
        } catch (e: GoogleJsonResponseException) {
            throw if (e.statusCode == 404) {
                SyncParseException(e)
            } else e
        }
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
        private const val IS_ATTACHMENT_FOLDER = "isAttachmentFolder"
        private const val IS_BUDGETS_FOLDER = "isBudgetsFolder"
        const val IS_SYNC_FOLDER = "isSyncFolder"

        private fun collectionPropertyKey(collectionName: String) =
            when (collectionName) {
                BACKUP_FOLDER_NAME -> IS_BACKUP_FOLDER
                ATTACHMENT_FOLDER_NAME -> IS_ATTACHMENT_FOLDER
                BUDGETS_FOLDER_NAME -> IS_BUDGETS_FOLDER
                else -> throw NotImplementedError()
            }
    }

    init {
        requireBaseFolder()
    }
}