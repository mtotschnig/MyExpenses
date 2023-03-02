package org.totschnig.myexpenses.sync

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.encrypted
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.notEncrypted
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException.Companion.wrongPassphrase
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.AdapterFactory
import org.totschnig.myexpenses.sync.json.CategoryExport
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.sync.json.Utils.getChanges
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.MIME_TYPE_OCTET_STREAM
import java.io.*
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.TimeUnit

abstract class AbstractSyncBackendProvider<Res>(protected val context: Context) :
    SyncBackendProvider, ShardingResourceStorage<Res> {
    /**
     * this holds the uuid of the db account which data is currently synced
     */
    @JvmField
    protected var accountUuid: String? = null
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
        get() = String.format("%s.%s", ACCOUNT_METADATA_FILENAME, extensionForData)
    val categoriesFilename: String
        get() = String.format("%s.%s", CATEGORIES_FILENAME, extensionForData)
    override val extensionForData: String
        get() = if (isEncrypted) "enc" else "json"

    fun setAccountUuid(account: Account) {
        accountUuid = account.uuid
    }

    protected abstract val sharedPreferencesName: String

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun initEncryption() {
        encryptionToken = encrypt(EncryptionHelper.generateRandom(10))

    }

    @Throws(Exception::class)
    override fun setUp(
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
                } catch (e: GeneralSecurityException) {
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

    @get:Throws(IOException::class)
    open var lockToken: String?
        get() = readFileContents(true, LOCK_FILE)
        set(value) {
            if (value == null) {
                deleteLockTokenFile()
            } else {
                saveFileContents(true, null, LOCK_FILE, value, "text/plain", false)
            }
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
    protected fun maybeEncrypt(inputStream: InputStream): InputStream {
        return try {
            if (isEncrypted) EncryptionHelper.encrypt(
                inputStream,
                encryptionPassword
            ) else inputStream
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    protected fun maybeDecrypt(inputStream: InputStream?): InputStream {
        return try {
            if (isEncrypted) EncryptionHelper.decrypt(
                inputStream,
                encryptionPassword
            ) else inputStream!!
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    protected fun toInputStream(fileContents: String, maybeEncrypt: Boolean): InputStream {
        val inputStream: InputStream = ByteArrayInputStream(fileContents.toByteArray())
        return if (maybeEncrypt) maybeEncrypt(inputStream) else inputStream
    }

    @Throws(IOException::class)
    protected fun getChangeSetFromInputStream(
        sequenceNumber: SequenceNumber,
        inputStream: InputStream
    ): ChangeSet {
        val changes: MutableList<TransactionChange>? =
            BufferedReader(InputStreamReader(maybeDecrypt(inputStream))).use { reader ->
                getChanges(gson, reader)
            }
        if (changes.isNullOrEmpty()) {
            return ChangeSet.empty(sequenceNumber)
        }
        val iterator = changes.listIterator()
        while (iterator.hasNext()) {
            val transactionChange = iterator.next()
            if (transactionChange.isEmpty) {
                log().w("found empty transaction change in json")
                iterator.remove()
            } else {
                iterator.set(mapPictureDuringRead(transactionChange))
                if (transactionChange.splitParts() != null) {
                    val splitPartIterator = transactionChange.splitParts()!!
                        .listIterator()
                    while (splitPartIterator.hasNext()) {
                        val splitPart = splitPartIterator.next()
                        splitPartIterator.set(mapPictureDuringRead(splitPart))
                    }
                }
            }
        }
        return ChangeSet.create(sequenceNumber, changes)
    }

    @Throws(IOException::class)
    private fun mapPictureDuringRead(transactionChange: TransactionChange): TransactionChange {
        transactionChange.pictureUri()?.let {
            val homeUri = PictureDirHelper.getOutputMediaUri(false)
                ?: throw IOException("Unable to write picture")
            val input = getInputStreamForPicture(it)
            val output = context.contentResolver
                .openOutputStream(homeUri) ?: throw IOException("Unable to write picture")
            FileCopyUtils.copy(maybeDecrypt(input), output)
            input.close()
            output.close()
            return transactionChange.toBuilder().setPictureUri(homeUri.toString()).build()
        }
        return transactionChange
    }

    @Throws(IOException::class)
    protected abstract fun getInputStreamForPicture(relativeUri: String): InputStream

    protected fun getAccountMetaDataFromInputStream(inputStream: InputStream?): Result<AccountMetaData> =
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

    abstract fun getChangeSetFromResource(shardNumber: Int, resource: Res): ChangeSet

    final override fun getChangeSetSince(sequenceNumber: SequenceNumber): ChangeSet? =
        merge(
            shardResolvingFilterStrategy(sequenceNumber).map {
                getChangeSetFromResource(it.first, it.second)
            }
        )

    @Throws(IOException::class)
    private fun mapPictureDuringWrite(transactionChange: TransactionChange): TransactionChange {
        if (transactionChange.pictureUri() != null) {
            val newUri: String = String.format(
                "%s_%s%s", transactionChange.uuid(),
                Uri.parse(transactionChange.pictureUri()).lastPathSegment,
                if (isEncrypted) ".enc" else ""
            )
            return try {
                saveUriToAccountDir(newUri, Uri.parse(transactionChange.pictureUri()))
                transactionChange.toBuilder().setPictureUri(newUri).build()
            } catch (e: IOException) {
                if (e is FileNotFoundException) {
                    log().w(e, "Picture was deleted, %s", transactionChange.pictureUri())
                    transactionChange.toBuilder().setPictureUri(null).build()
                } else {
                    throw e
                }
            }
        }
        return transactionChange
    }

    @Throws(IOException::class)
    override fun writeChangeSet(
        lastSequenceNumber: SequenceNumber,
        changeSet: List<TransactionChange>,
        context: Context
    ): SequenceNumber {
        val changeSetMutable = changeSet.toMutableList()
        val nextSequence = getLastSequence(lastSequenceNumber).next()
        for (i in changeSetMutable.indices) {
            var mappedChange = mapPictureDuringWrite(changeSetMutable[i])
            if (appInstance != null) {
                mappedChange = mappedChange.toBuilder().setAppInstance(appInstance).build()
            }
            mappedChange.splitParts()?.toMutableList()?.let { splitPartsMutable ->
                for (j in splitPartsMutable.indices) {
                    splitPartsMutable[j] = mapPictureDuringWrite(splitPartsMutable[j])
                }
                mappedChange = mappedChange.toBuilder().setSplitParts(splitPartsMutable).build()
            }
            changeSetMutable[i] = mappedChange
        }
        val fileName = String.format(Locale.ROOT, "_%d.%s", nextSequence.number, extensionForData)
        val fileContents = gson.toJson(changeSetMutable)
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
    protected abstract fun saveUriToAccountDir(fileName: String, uri: Uri)

    protected fun buildMetadata(account: Account?): String {
        return gson.toJson(AccountMetaData.from(account))
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
        fileName: String
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

    override fun writeCategories(categories: List<CategoryExport>): String {
        saveFileContents(false, null, categoriesFilename, gson.toJson(categories), mimeTypeForData, true)
        return categoriesFilename
    }

    override val categories: Result<List<CategoryExport>>
        get() = kotlin.runCatching {
            readFileContents(false, categoriesFilename)?.let {
                    gson.fromJson<List<CategoryExport>>(it, object : TypeToken<ArrayList<CategoryExport>>() {}.type)
            } ?: throw FileNotFoundException(context.getString(R.string.not_exist_file_desc) + ": " + categoriesFilename)
        }

    @Throws(IOException::class)
    protected abstract fun writeAccount(account: Account, update: Boolean)

    @Throws(IOException::class)
    override fun lock() {
        val existingLockToken = lockToken
        log().i("ExistingLockToken: %s", existingLockToken)
        if (TextUtils.isEmpty(existingLockToken) || shouldOverrideLock(existingLockToken)) {
            val newLockToken = Model.generateUuid()
            lockToken = newLockToken
            saveLockTokenToPreferences(newLockToken, System.currentTimeMillis(), true)
        } else {
            throw IOException("Backend cannot be locked")
        }
    }

    override fun unlock() {
        lockToken = null
    }

    private fun shouldOverrideLock(lockToken: String?): Boolean {
        val result: Boolean
        val now = System.currentTimeMillis()
        val storedLockToken = sharedPreferences.getString(accountPrefKey(KEY_LOCK_TOKEN), "")
        val ownedByUs = sharedPreferences.getBoolean(accountPrefKey(KEY_OWNED_BY_US), false)
        val timestamp = sharedPreferences.getLong(accountPrefKey(KEY_TIMESTAMP), 0)
        val since = now - timestamp
        log().i("Stored: %s, ownedByUs : %b, since: %d", storedLockToken, ownedByUs, since)
        if (lockToken == storedLockToken) {
            result = ownedByUs || since > LOCK_TIMEOUT_MILLIS
            log().i("tokens are equal, result: %b", result)
        } else {
            saveLockTokenToPreferences(lockToken, now, false)
            result = false
            log().i("tokens are not equal, result: %b", false)
        }
        return result
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

    companion object {
        const val LOCK_FILE = ".lock.txt"
        const val KEY_LOCK_TOKEN = "lockToken"
        const val BACKUP_FOLDER_NAME = "BACKUPS"
        const val MIME_TYPE_JSON = "application/json"
        private const val ACCOUNT_METADATA_FILENAME = "metadata"
        private const val CATEGORIES_FILENAME = "categories"
        private const val KEY_OWNED_BY_US = "ownedByUs"
        private const val KEY_TIMESTAMP = "timestamp"
        private val LOCK_TIMEOUT_MILLIS =
            TimeUnit.MINUTES.toMillis(SyncAdapter.LOCK_TIMEOUT_MINUTES.toLong())
        const val ENCRYPTION_TOKEN_FILE_NAME = "ENCRYPTION_TOKEN"
    }

    init {
        if (BuildConfig.DEBUG) {
            appInstance =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}