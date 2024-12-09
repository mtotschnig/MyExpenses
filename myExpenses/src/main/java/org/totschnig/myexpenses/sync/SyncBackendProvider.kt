package org.totschnig.myexpenses.sync

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.BudgetExport
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.model2.CategoryExport
import org.totschnig.myexpenses.sync.json.ChangeSet
import org.totschnig.myexpenses.sync.json.TransactionChange
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException

interface SyncBackendProvider {
    @Throws(IOException::class)
    fun withAccount(account: Account)

    @Throws(IOException::class)
    fun resetAccountData(uuid: String)

    @Throws(IOException::class)
    fun lock()

    @Throws(IOException::class)
    fun unlock()

    @Throws(IOException::class)
    fun getChangeSetSince(sequenceNumber: SequenceNumber): ChangeSet?

    @Throws(IOException::class)
    fun writeChangeSet(
        lastSequenceNumber: SequenceNumber,
        changeSet: List<TransactionChange>,
        context: Context
    ): SequenceNumber

    val remoteAccountList: List<Result<AccountMetaData>>
        @Throws(IOException::class) get

    @Throws(Exception::class)
    suspend fun setUp(
        accountManager: AccountManager,
        account: android.accounts.Account,
        encryptionPassword: String?,
        create: Boolean
    )

    @Throws(IOException::class)
    fun storeBackup(uri: Uri, fileName: String)

    @get:Throws(IOException::class)
    val storedBackups: List<String>

    @Throws(IOException::class)
    fun getInputStreamForBackup(backupFile: String): InputStream

    @Throws(GeneralSecurityException::class, IOException::class)
    fun initEncryption()

    @Throws(IOException::class)
    fun updateAccount(account: Account)

    fun readAccountMetaData(): Result<AccountMetaData>

    fun writeCategories(categories: List<CategoryExport>): String

    val categories: Result<List<CategoryExport>>

    val budgets: List<Pair<String, BudgetExport>>

    fun writeBudget(uuid: String, budget: BudgetExport): String

    @Throws(IOException::class)
    fun getBudget(uuid: String): BudgetExport

    fun suggestDelay(e: IOException, defaultDelay: Long): Long = defaultDelay

    class SyncParseException : Exception {
        constructor(e: Exception) : super(e.message, e)
        constructor(message: String) : super(message)
    }

    class AuthException(cause: Throwable, val resolution: Intent?) : IOException(cause)

    class EncryptionException private constructor(message: String) : Exception(message) {
        companion object {
            @JvmStatic
            fun notEncrypted(context: Context): EncryptionException {
                return EncryptionException(context.getString(R.string.sync_backend_is_not_encrypted))
            }

            @JvmStatic
            fun encrypted(context: Context): EncryptionException {
                return EncryptionException(context.getString(R.string.sync_backend_is_encrypted))
            }

            @JvmStatic
            fun wrongPassphrase(context: Context): EncryptionException {
                return EncryptionException(context.getString(R.string.sync_backend_wrong_passphrase))
            }
        }
    }
}