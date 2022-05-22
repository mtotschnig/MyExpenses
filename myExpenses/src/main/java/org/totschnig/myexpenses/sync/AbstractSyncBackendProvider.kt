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
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.MIME_TYPE_OCTET_STREAM
import org.totschnig.myexpenses.util.io.getFileExtension
import org.totschnig.myexpenses.util.io.getNameWithoutExtension
import timber.log.Timber
import java.io.*
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

abstract class AbstractSyncBackendProvider(protected val context: Context) : SyncBackendProvider {
    /**
     * this holds the uuid of the db account which data is currently synced
     */
    @JvmField
    protected var accountUuid: String? = null
    var sharedPreferences: SharedPreferences
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create()
    private var appInstance: String? = null
    private var encryptionPassword: String? = null
    val mimeTypeForData: String
        get() = if (isEncrypted) MIME_TYPE_JSON else MIME_TYPE_OCTET_STREAM
    protected val isEncrypted: Boolean
        get() = encryptionPassword != null
    val accountMetadataFilename: String
        get() = String.format("%s.%s", ACCOUNT_METADATA_FILENAME, extensionForData)
    private val extensionForData: String
        get() = if (isEncrypted) "enc" else "json"

    fun setAccountUuid(account: Account) {
        accountUuid = account.uuid
    }

    protected abstract val sharedPreferencesName: String

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun initEncryption() {
        saveFileContentsToBase(
            ENCRYPTION_TOKEN_FILE_NAME,
            encrypt(EncryptionHelper.generateRandom(10)), MIME_TYPE_OCTET_STREAM, false
        )
    }

    @Throws(Exception::class)
    override fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    ) {
        this.encryptionPassword = encryptionPassword
        val encryptionToken = readEncryptionToken()
        if (encryptionToken == null) {
            if (encryptionPassword != null) {
                if (create && isEmpty) {
                    initEncryption()
                } else {
                    throw notEncrypted(context)
                }
            }
        } else {
            if (encryptionPassword == null) {
                throw encrypted(context)
            } else {
                try {
                    decrypt(encryptionToken)
                } catch (e: GeneralSecurityException) {
                    throw wrongPassphrase(context)
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

    @Throws(IOException::class)
    protected abstract fun readEncryptionToken(): String?

    @Throws(GeneralSecurityException::class)
    protected fun encrypt(plain: ByteArray?): String {
        return Base64.encodeToString(
            EncryptionHelper.encrypt(plain, encryptionPassword),
            Base64.DEFAULT
        )
    }

    @Throws(IOException::class)
    protected fun maybeEncrypt(outputStream: OutputStream?): OutputStream {
        return try {
            if (isEncrypted) EncryptionHelper.encrypt(
                outputStream,
                encryptionPassword
            ) else outputStream!!
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
            org.totschnig.myexpenses.sync.json.Utils.getChanges(gson, reader)
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
                    val jterator = transactionChange.splitParts()!!
                        .listIterator()
                    while (jterator.hasNext()) {
                        val splitPart = jterator.next()
                        jterator.set(mapPictureDuringRead(splitPart))
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

    protected fun isAtLeastShardDir(shardNumber: Int, name: String): Boolean {
        return FILE_PATTERN.matcher(name).matches() &&
                name.substring(1).toInt() >= shardNumber
    }

    protected fun isNewerJsonFile(sequenceNumber: Int, name: String): Boolean {
        val fileName = getNameWithoutExtension(name)
        val fileExtension = getFileExtension(name)
        return fileExtension == extensionForData && FILE_PATTERN.matcher(fileName)
            .matches() && fileName.substring(1).toInt() > sequenceNumber
    }

    protected fun merge(changeSetList: List<ChangeSet>): ChangeSet? {
        return changeSetList.reduceOrNull { changeSet1: ChangeSet?, changeSet2: ChangeSet? ->
            ChangeSet.merge(changeSet1, changeSet2)
        }
    }

    protected fun getSequenceFromFileName(fileName: String): Int {
        return try {
            getNameWithoutExtension(fileName).takeIf { it.isNotEmpty() && it.startsWith("_") }?.substring(1)?.toInt()
        } catch (e: NumberFormatException) { null } ?: 0
    }

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
        saveFileContentsToAccountDir(
            if (nextSequence.shard == 0) null else "_" + nextSequence.shard,
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
    protected abstract fun getLastSequence(start: SequenceNumber): SequenceNumber

    @Throws(IOException::class)
    protected abstract fun saveFileContentsToAccountDir(
        folder: String?,
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    )

    @Suppress("SameParameterValue")
    @Throws(IOException::class)
    protected abstract fun saveFileContentsToBase(
        fileName: String,
        fileContents: String,
        mimeType: String,
        maybeEncrypt: Boolean
    )

    protected fun createWarningFile() {
        try {
            saveFileContentsToAccountDir(
                null, "IMPORTANT_INFORMATION.txt",
                Utils.getTextWithAppName(context, R.string.warning_synchronization_folder_usage)
                    .toString(),
                "text/plain", false
            )
        } catch (e: IOException) {
            log().w(e)
        }
    }

    @get:Throws(IOException::class)
    protected abstract val existingLockToken: String?

    @Throws(IOException::class)
    protected abstract fun writeLockToken(lockToken: String)

    @Throws(IOException::class)
    override fun updateAccount(account: Account) {
        writeAccount(account, true)
    }

    @Throws(IOException::class)
    protected abstract fun writeAccount(account: Account, update: Boolean)

    @Throws(IOException::class)
    override fun lock() {
        val existingLockToken = existingLockToken
        log().i("ExistingLockToken: %s", existingLockToken)
        if (TextUtils.isEmpty(existingLockToken) || shouldOverrideLock(existingLockToken)) {
            val lockToken = Model.generateUuid()
            writeLockToken(lockToken)
            saveLockTokenToPreferences(lockToken, System.currentTimeMillis(), true)
        } else {
            throw IOException("Backend cannot be locked")
        }
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

    protected fun log(): Timber.Tree {
        return Timber.tag(SyncAdapter.TAG)
    }

    companion object {
        const val KEY_LOCK_TOKEN = "lockToken"
        const val BACKUP_FOLDER_NAME = "BACKUPS"
        private const val MIME_TYPE_JSON = "application/json"
        private const val ACCOUNT_METADATA_FILENAME = "metadata"
        protected val FILE_PATTERN: Pattern = Pattern.compile("_\\d+")
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
        sharedPreferences = context.getSharedPreferences(sharedPreferencesName, 0)
    }
}