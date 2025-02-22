package org.totschnig.myexpenses.sync

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import androidx.annotation.CallSuper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.model2.CategoryExport
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.fileName
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.sync.SyncAdapter.Companion.IO_LOCK_DELAY_MILLIS
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.encrypted
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.notEncrypted
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.wrongPassphrase
import org.totschnig.myexpenses.sync.json.*
import org.totschnig.myexpenses.sync.json.Utils.getChanges
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.MIME_TYPE_OCTET_STREAM
import org.totschnig.myexpenses.util.io.getFileExtension
import org.totschnig.myexpenses.util.io.getNameWithoutExtension
import java.io.*
import java.security.GeneralSecurityException
import java.util.*

abstract class AbstractSyncBackendProvider<Res>(protected val context: Context) :
    SyncBackendProvider, ResourceStorage<Res> {
    /**
     * this holds the uuid of the db account which data is currently synced
     */
    lateinit var accountUuid: String
    val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${sharedPreferencesName}_sync", 0)
    }
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create()
    private var appInstance: String? = null
    private var encryptionPassword: String? = null
    val mimeTypeForData: String
        get() = if (isEncrypted) MIME_TYPE_OCTET_STREAM else MIME_TYPE_JSON
    protected val isEncrypted: Boolean
        get() = encryptionPassword != null
    val accountMetadataFilename: String
        get() = "$ACCOUNT_METADATA_FILENAME.$extensionForData"
    private val categoriesFilename: String
        get() = "$CATEGORIES_FILENAME.$extensionForData"
    override val extensionForData: String
        get() = if (isEncrypted) "enc" else "json"

    abstract val accountRes: Res

    fun setAccountUuid(account: Account) {
        accountUuid = account.uuid ?: throw IllegalArgumentException("uuid is null")
    }

    protected abstract val sharedPreferencesName: String

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun initEncryption() {
        encryptionToken = encrypt(EncryptionHelper.generateRandom(10))

    }

    @Throws(Exception::class)
    override suspend fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        this.encryptionPassword = encryptionPassword
        encryptionToken?.also {
            if (encryptionPassword == null) {
                throw encrypted(context)
            } else {
                try {
                    decrypt(it)
                } catch (_: GeneralSecurityException) {
                    throw wrongPassphrase(context)
                }
            }
        } ?: run {
            if (encryptionPassword != null) {
                if (create && isEmpty) {
                    initEncryption()
                } else {
                    throw notEncrypted(context)
                }
            }
        }
    }

    @get:Throws(IOException::class)
    protected abstract val isEmpty: Boolean

    @Throws(GeneralSecurityException::class)
    private fun decrypt(input: String): String {
        return String(
            EncryptionHelper.decrypt(
                Base64.decode(input, Base64.DEFAULT),
                encryptionPassword
            )
        )
    }

    @get:Throws(IOException::class)
    protected var encryptionToken: String?
        get() = readFileContents(false, ENCRYPTION_TOKEN_FILE_NAME)
        set(value) {
            saveFileContents(
                false, null,
                ENCRYPTION_TOKEN_FILE_NAME,
                value!!, MIME_TYPE_OCTET_STREAM, false
            )
        }

    open fun deleteLockTokenFile() {
        throw IllegalStateException("Should be handled by implementation")
    }

    open fun getLockToken() = readFileContents(true, LOCK_FILE)
    open fun setLockToken(lockToken: String) {
        saveFileContents(true, null, LOCK_FILE, lockToken, "text/plain", false)
    }

    @Throws(GeneralSecurityException::class)
    protected fun encrypt(plain: ByteArray?): String {
        return Base64.encodeToString(
            EncryptionHelper.encrypt(plain, encryptionPassword),
            Base64.DEFAULT
        )
    }

    @Throws(IOException::class)
    protected fun maybeEncrypt(outputStream: OutputStream): OutputStream {
        return try {
            if (isEncrypted) EncryptionHelper.encrypt(
                outputStream,
                encryptionPassword
            ) else outputStream
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    protected fun maybeEncrypt(
        inputStream: InputStream,
        maybeEncrypt: Boolean = true
    ): InputStream =
        if (maybeEncrypt && isEncrypted) try {
            EncryptionHelper.encrypt(
                inputStream,
                encryptionPassword
            )
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        } else inputStream

    @Throws(IOException::class)
    protected fun maybeDecrypt(
        inputStream: InputStream,
        maybeDecrypt: Boolean = true
    ) = try {
        if (maybeDecrypt && isEncrypted) EncryptionHelper.decrypt(
            inputStream,
            encryptionPassword
        ) else inputStream
    } catch (e: GeneralSecurityException) {
        throw IOException(e)
    }

    @Throws(IOException::class)
    protected fun toInputStream(fileContents: String, maybeEncrypt: Boolean) =
        maybeEncrypt(ByteArrayInputStream(fileContents.toByteArray()), maybeEncrypt)

    @Throws(IOException::class)
    protected fun getChangeSetFromInputStream(
        sequenceNumber: SequenceNumber,
        inputStream: InputStream
    ): ChangeSet {
        log().i("getChangeSetFromInputStream for $sequenceNumber")
        val changes: MutableList<TransactionChange>? =
            BufferedReader(InputStreamReader(maybeDecrypt(inputStream))).use { reader ->
                getChanges(gson, reader)
            }
        if (changes.isNullOrEmpty()) {
            return ChangeSet.empty(sequenceNumber)
        }
        val iterator = changes.listIterator()
        ensureAttachmentsOnRead(changes)
        while (iterator.hasNext()) {
            val transactionChange = iterator.next()
            if (transactionChange.isEmpty) {
                log().w("found empty transaction change in json")
                iterator.remove()
            } else {
                transactionChange.pictureUri()?.let {
                    if (transactionChange.attachments()?.isNotEmpty() == true) {
                        CrashHandler.report(IllegalStateException("found attachments and legacy pictureUri together"))
                    } else {
                        iterator.set(transactionChange.toBuilder().setAttachments(
                            setOf(mapLegacyPictureDuringRead(it))
                        ).build())
                    }
                }
            }
        }
        return ChangeSet.create(sequenceNumber, changes)
    }

    abstract fun getResInAccountDir(resourceName: String): Res?

    final override fun collectionForShard(shardNumber: Int) =
        if (shardNumber == 0) accountRes else getResInAccountDir(folderForShard(shardNumber))

    @Throws(IOException::class)
    private fun mapLegacyPictureDuringRead(uri: String) = Model.generateUuid().also {
        storeAttachmentToDatabase(uri, it, getInputStreamForLegacyPicture(uri))
    }

    private fun storeAttachmentToDatabase(fileName: String, uuid:String, inputStream: InputStream) {
        val homeUri = PictureDirHelper.getOutputMediaUri(
            false,
            context.myApplication,
            fileName = getNameWithoutExtension(fileName),
            extension = getFileExtension(fileName)
        )
        val output = context.contentResolver
            .openOutputStream(homeUri) ?: throw IOException("Unable to write picture")
        FileCopyUtils.copy(maybeDecrypt(inputStream), output)
        inputStream.close()
        output.close()
        context.contentResolver.insert(
            TransactionProvider.ATTACHMENTS_URI,
            ContentValues(2).apply {
                put(KEY_URI, homeUri.toString())
                put(KEY_UUID, uuid)
            }
        )
    }

    private fun ensureAttachmentsOnRead(changeSet: List<TransactionChange>) {
        val attachments = changeSet.flatMap { it.attachments() ?: emptyList() }.toSet()

        if (attachments.isEmpty()) return
        //noinspection Recycle
        val existing = context.contentResolver.query(
            TransactionProvider.ATTACHMENTS_URI,
            arrayOf(KEY_UUID),
            "$KEY_UUID ${Operation.IN.getOp(attachments.size)}",
            attachments.toTypedArray(),
            null
        )?.useAndMapToList { it.getString(0) } ?: emptyList()
        log().w("ensureAttachmentsOnRead: found %s", existing.joinToString())
        (attachments - existing.toSet()).forEach { uuid ->
            val (fileName, inputStream) = getAttachment(uuid)
            storeAttachmentToDatabase(fileName, uuid, inputStream)
        }

    }

    @Throws(IOException::class)
    fun getInputStreamForLegacyPicture(relativeUri: String) =
        getInputStream(getResInAccountDir(relativeUri) ?: throw FileNotFoundException())

    @Throws(IOException::class)
    fun getAttachment(uuid: String): Pair<String, InputStream> {
        val attachmentDir = requireCollection(ATTACHMENT_FOLDER_NAME)
        val attachment = childrenForCollection(attachmentDir).find { nameForResource(it)?.startsWith(uuid) == true } ?: throw FileNotFoundException()
        return nameForResource(attachment)!!.substringAfter("${uuid}_") to getInputStream(attachment)
    }

    private fun storeAttachmentToBackendIfNeeded(uuid: String, uri: Uri, fileName: String) {
        val attachmentDir = requireCollection(ATTACHMENT_FOLDER_NAME)
        if (childrenForCollection(attachmentDir).none { nameForResource(it)?.startsWith(uuid) == true }) {
            saveUriToCollection("${uuid}_$fileName", uri, attachmentDir)
        }
    }

    protected fun getAccountMetaDataFromInputStream(inputStream: InputStream): Result<AccountMetaData> =
        try {
            BufferedReader(InputStreamReader(maybeDecrypt(inputStream))).use { bufferedReader ->
                val accountMetaData = gson.fromJson(bufferedReader, AccountMetaData::class.java)
                    ?: throw IOException("accountMetaData not found in input stream")
                Result.success(accountMetaData)
            }
        } catch (e: Exception) {
            log().e(e)
            Result.failure(e)
        }

    protected fun merge(changeSetList: List<ChangeSet>): ChangeSet? {
        return changeSetList.reduceOrNull { changeSet1: ChangeSet?, changeSet2: ChangeSet? ->
            ChangeSet.merge(changeSet1, changeSet2)
        }
    }

    private fun getChangeSetFromResource(shardNumber: Int, resource: Res): ChangeSet = getChangeSetFromInputStream(
        SequenceNumber(shardNumber, getSequenceFromFileName(nameForResource(resource))),
        getInputStream(resource)
    )

    final override fun getChangeSetSince(sequenceNumber: SequenceNumber): ChangeSet? =
        merge(
            shardResolvingFilterStrategy(sequenceNumber).map {
                getChangeSetFromResource(it.first, it.second)
            }
        )

    @Throws(IOException::class)
    private fun ensureAttachmentsOnWrite(changeSet: List<TransactionChange>) {
        val attachments = changeSet.flatMap { it.attachments() ?: emptyList() }.toSet()
        if (attachments.isNotEmpty()) {
            context.contentResolver.query(
                TransactionProvider.ATTACHMENTS_URI,
                arrayOf(KEY_UUID, KEY_URI),
                "$KEY_UUID ${Operation.IN.getOp(attachments.size)}",
                attachments.toTypedArray(),
                null
            )?.use { cursor ->
                cursor.asSequence.forEach {
                    val uuid = it.getString(0)
                    val attachmentUri = Uri.parse(it.getString(1))
                    storeAttachmentToBackendIfNeeded(uuid, attachmentUri, attachmentUri.fileName(context))
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun writeChangeSet(
        lastSequenceNumber: SequenceNumber,
        changeSet: List<TransactionChange>,
        context: Context
    ): SequenceNumber {
        val nextSequence = getLastSequence(lastSequenceNumber).next()
        val finalChangeSet = if (appInstance != null) {
            changeSet.map { it.toBuilder().setAppInstance(appInstance).build() }
        } else changeSet

        val fileName = "_${nextSequence.number}.$extensionForData"
        val fileContents = gson.toJson(finalChangeSet)
        ensureAttachmentsOnWrite(finalChangeSet)
        log().i("Writing to %s", fileName)
        log().i(fileContents)
        saveFileContents(
            true,
            if (nextSequence.shard == 0) null else folderForShard(nextSequence.shard),
            fileName,
            fileContents,
            mimeTypeForData,
            true
        )
        return nextSequence
    }


    /**
     * should encrypt if backend is configured with encryption
     */
    @Throws(IOException::class)
    protected abstract fun saveUriToCollection(fileName: String, uri: Uri, collection: Res, maybeEncrypt: Boolean = true)

    protected fun buildMetadata(account: Account?): String {
        return gson.toJson(
            AccountMetaData.from(
                account,
                context.injector.currencyContext().homeCurrencyString
            )
        )
    }

    protected fun getLastFileNamePart(fileName: String): String {
        return if (fileName.contains("/")) StringUtils.substringAfterLast(
            fileName,
            "/"
        ) else fileName
    }

    @Throws(IOException::class)
    protected abstract fun saveFileContents(
        toAccountDir: Boolean,
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    )

    protected abstract fun readFileContents(
        fromAccountDir: Boolean,
        fileName: String,
        maybeDecrypt: Boolean = false
    ): String?

    protected fun createWarningFile() {
        try {
            saveFileContents(
                true, null,
                "IMPORTANT_INFORMATION.txt",
                Utils.getTextWithAppName(context, R.string.warning_synchronization_folder_usage)
                    .toString(), "text/plain", false
            )
        } catch (e: IOException) {
            log().w(e)
        }
    }

    @Throws(IOException::class)
    override fun updateAccount(account: Account) {
        writeAccount(account, true)
    }

    final override fun storeBackup(uri: Uri, fileName: String) {
        saveUriToCollection(fileName, uri, requireCollection(BACKUP_FOLDER_NAME), false)
    }

    final override val storedBackups: List<String>
        get() = getCollection(BACKUP_FOLDER_NAME)?.let { folder ->
            childrenForCollection(folder).mapNotNull { nameForResource(it) }
        } ?: emptyList()

    final override fun getInputStreamForBackup(backupFile: String) = getInputStream(
    childrenForCollection(requireCollection(BACKUP_FOLDER_NAME))
        .find { nameForResource(it) == backupFile } ?: throw FileNotFoundException()
    )

    override fun writeCategories(categories: List<CategoryExport>): String {
        saveFileContents(
            false,
            null,
            categoriesFilename,
            gson.toJson(categories),
            mimeTypeForData,
            true
        )
        return categoriesFilename
    }

    override fun writeBudget(uuid: String, budget: BudgetExport): String {
        val fileName = "$uuid.$extensionForData"
        requireCollection(BUDGETS_FOLDER_NAME)
        saveFileContents(
            false,
            BUDGETS_FOLDER_NAME,
            fileName,
            Json.encodeToString(budget),
            mimeTypeForData,
            true
        )
        return fileName
    }

    override val categories: Result<List<CategoryExport>>
        get() = kotlin.runCatching {
            readFileContents(false, categoriesFilename, true)?.let {
                gson.fromJson<List<CategoryExport>>(
                    it,
                    object : TypeToken<ArrayList<CategoryExport>>() {}.type
                )
            }
                ?: throw FileNotFoundException(context.getString(R.string.not_exist_file_desc) + ": " + categoriesFilename)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override val budgets: List<Pair<String, BudgetExport>>
        get() = getCollection(BUDGETS_FOLDER_NAME)?.let { folder ->
            childrenForCollection(folder)
                .mapNotNull { res ->
                    nameForResource(res)?.let { getNameWithoutExtension(it) }?.let { uuid ->
                        maybeDecrypt(getInputStream(res))?.let {
                            uuid to Json.decodeFromStream(it)
                        }
                    }
                }
        } ?: emptyList()

    final override fun getBudget(uuid: String): BudgetExport {
        val inputStream = getInputStream(
            childrenForCollection(requireCollection(BUDGETS_FOLDER_NAME))
                .find { nameForResource(it) == "$uuid.$extensionForData" } ?: throw FileNotFoundException()
        )

        return BufferedReader(InputStreamReader(maybeDecrypt(inputStream))).use { bufferedReader ->
            gson.fromJson(bufferedReader, BudgetExport::class.java)
        }
    }

    @CallSuper
    override fun withAccount(account: Account) {
        setAccountUuid(account)
    }

    @Throws(IOException::class)
    protected abstract fun writeAccount(account: Account, update: Boolean)

    @Throws(IOException::class)
    override fun lock() {
        val existingLockToken = getLockToken()
        log().i("ExistingLockToken: %s", existingLockToken)
        if (TextUtils.isEmpty(existingLockToken) || shouldOverrideLock(existingLockToken)) {
            val newLockToken = Model.generateUuid()
            setLockToken(newLockToken)
            saveLockTokenToPreferences(newLockToken, System.currentTimeMillis(), true)
        } else {
            throw IOException("Backend cannot be locked")
        }
    }

    override fun unlock() {
        deleteLockTokenFile()
    }

    private fun shouldOverrideLock(lockToken: String?): Boolean {
        val now = System.currentTimeMillis()
        val storedLockToken = sharedPreferences.getString(accountPrefKey(KEY_LOCK_TOKEN), "")
        val ownedByUs = sharedPreferences.getBoolean(accountPrefKey(KEY_OWNED_BY_US), false)
        val timestamp = sharedPreferences.getLong(accountPrefKey(KEY_TIMESTAMP), 0)
        val since = now - timestamp
        log().i("Stored: %s, ownedByUs : %b, since: %d", storedLockToken, ownedByUs, since)
        return if (lockToken == storedLockToken) {
            (ownedByUs || since > IO_LOCK_DELAY_MILLIS).also {
                log().i("tokens are equal, result: %b", it)
            }
        } else {
            saveLockTokenToPreferences(lockToken, now, false)
            log().i("tokens are not equal, result: false")
            false
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun saveLockTokenToPreferences(
        lockToken: String?,
        timestamp: Long,
        ownedByUs: Boolean
    ) {
        sharedPreferences.edit().putString(accountPrefKey(KEY_LOCK_TOKEN), lockToken)
            .putLong(accountPrefKey(KEY_TIMESTAMP), timestamp)
            .putBoolean(accountPrefKey(KEY_OWNED_BY_US), ownedByUs).commit()
    }

    private fun accountPrefKey(key: String): String {
        return String.format(Locale.ROOT, "%s-%s", accountUuid, key)
    }

    protected fun verifyRemoteAccountFolderName(folderName: String?) =
        folderName != null && folderName !in specialFolders

    companion object {
        const val LOCK_FILE = ".lock.txt"
        const val KEY_LOCK_TOKEN = "lockToken"
        const val BACKUP_FOLDER_NAME = "BACKUPS"
        const val ATTACHMENT_FOLDER_NAME = "ATTACHMENTS"
        const val BUDGETS_FOLDER_NAME ="BUDGETS_V2"
        //BUDGETS was used for first broken version of Budget sync
        val specialFolders = listOf(BACKUP_FOLDER_NAME, ATTACHMENT_FOLDER_NAME, "BUDGETS", BUDGETS_FOLDER_NAME)
        const val MIME_TYPE_JSON = "application/json"
        private const val ACCOUNT_METADATA_FILENAME = "metadata"
        private const val CATEGORIES_FILENAME = "categories"
        private const val KEY_OWNED_BY_US = "ownedByUs"
        private const val KEY_TIMESTAMP = "timestamp"
        const val ENCRYPTION_TOKEN_FILE_NAME = "ENCRYPTION_TOKEN"
    }

    init {
        if (BuildConfig.DEBUG) {
            appInstance =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}