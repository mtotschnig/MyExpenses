package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.accounts.AccountManager.KEY_AUTHTOKEN
import android.accounts.AccountManager.KEY_PASSWORD
import android.accounts.AccountManager.KEY_USERDATA
import android.accounts.AccountManager.get
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.findAccountByUuid
import org.totschnig.myexpenses.db2.requireAccountTypeForSync
import org.totschnig.myexpenses.db2.storeExchangeRate
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.KEY_SYNC_PROVIDER_URL
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.KEY_SYNC_PROVIDER_USERNAME
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getSyncBackendProvider
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException

open class SyncViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    fun syncLinkRemote(account: Account): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            val accountId = repository.findAccountByUuid(account.uuid!!)
            if (accountId == null) {
                emit(Result.failure(IllegalStateException("Account with uuid ${account.uuid} not found")))
            } else {
                emit(deleteAccountsInternal(longArrayOf(accountId)).also {
                    it.onSuccess {
                        doSave(account)
                    }
                })
            }
        }

    fun syncLinkLocal(accountName: String, uuid: String): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            try {
                configureLocalAccountForSync(accountName, uuid)
            } catch (e: SQLiteConstraintException) {
                emit(Result.failure(AccountSealedException()))
            }
            resetRemote(accountName, uuid)
            emit(ResultUnit)
        }


    fun resetRemote(accountName: String, uuid: String) {
        GenericAccountService.requestSync(accountName, uuid = uuid, extras = Bundle().apply {
            putBoolean(SyncAdapter.KEY_RESET_REMOTE_ACCOUNT, true)
        })
    }

    protected fun doSave(accountIn: Account) {
        val accountType = repository.requireAccountTypeForSync(accountIn.type.name)

        val account = repository.createAccount(
            accountIn.copy(type = accountType)
        )
        val homeCurrency = currencyContext.homeCurrencyUnit
        repository.storeExchangeRate(
            account.id,
            account.exchangeRate,
            account.currency,
            homeCurrency.code
        )
        licenceHandler.updateNewAccountEnabled()
        updateTransferShortcut()
    }

    fun getReconfigurationData(syncAccount: String) = Bundle().apply {
        val accountManager = get(getApplication())
        val account = getAccount(syncAccount)
        putString(KEY_ORIGINAL_ACCOUNT_NAME, syncAccount)
        putString(KEY_PASSWORD, accountManager.getPassword(account))
        putString(KEY_SYNC_PROVIDER_URL, accountManager.getUserData(account, KEY_SYNC_PROVIDER_URL))
        putString(
            KEY_SYNC_PROVIDER_USERNAME,
            accountManager.getUserData(account, KEY_SYNC_PROVIDER_USERNAME)
        )
    }

    fun reconfigure(data: Bundle): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            val accountManager = get(getApplication())
            val oldName = data.getString(KEY_ORIGINAL_ACCOUNT_NAME)!!
            var account = getAccount(oldName)
            val newName = data.getString(KEY_ACCOUNT_NAME)
            if (data.getString(KEY_ORIGINAL_ACCOUNT_NAME) != newName) {
                val accountManagerFuture =
                    accountManager.renameAccount(account, newName, null, null)
                account = accountManagerFuture.result
                if (account.name != newName) emit(false)
                val contentValues = ContentValues(1).apply {
                    put(KEY_SYNC_ACCOUNT_NAME, newName)
                }
                contentResolver.update(
                    TransactionProvider.ACCOUNTS_URI,
                    contentValues,
                    "$KEY_SYNC_ACCOUNT_NAME = ?",
                    arrayOf(oldName)
                )
            }
            accountManager.setPassword(account, data.getString(KEY_PASSWORD))
            val userData = data.getBundle(KEY_USERDATA)!!
            accountManager.setUserData(
                account,
                KEY_SYNC_PROVIDER_URL,
                userData.getString(KEY_SYNC_PROVIDER_URL)
            )
            accountManager.setUserData(
                account,
                KEY_SYNC_PROVIDER_USERNAME,
                userData.getString(KEY_SYNC_PROVIDER_USERNAME)
            )
            emit(true)
        }

    fun createSyncAccount(
        args: Bundle,
        shouldQueryLocalAccounts: Boolean,
        shouldReturnBackups: Boolean,
        shouldQueryRemoteAccounts: Boolean
    ): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            val accountName = args.getString(KEY_ACCOUNT_NAME)!!
            val password = args.getString(KEY_PASSWORD)
            val userData = args.getBundle(KEY_USERDATA)
            val authToken = args.getString(KEY_AUTHTOKEN)
            val encryptionPassword = args.getString(GenericAccountService.KEY_PASSWORD_ENCRYPTION)
            val accountManager = get(getApplication())
            val account = getAccount(accountName)
            emit(if (accountManager.addAccountExplicitly(account, password, userData)) {
                if (authToken != null) {
                    accountManager.setAuthToken(
                        account,
                        GenericAccountService.AUTH_TOKEN_TYPE,
                        authToken
                    )
                }
                if (encryptionPassword != null) {
                    accountManager.setUserData(
                        account,
                        GenericAccountService.KEY_ENCRYPTED,
                        java.lang.Boolean.toString(true)
                    )
                }
                GenericAccountService.storePassword(
                    getApplication(),
                    account,
                    encryptionPassword
                )
                buildResult(
                    accountName,
                    shouldReturnBackups,
                    shouldQueryLocalAccounts,
                    true,
                    shouldQueryRemoteAccounts
                ).onFailure {
                    //we try to remove a failed account immediately, otherwise user would need to do it, before
                    //being able to try again
                    @Suppress("DEPRECATION") val accountManagerFuture =
                        accountManager.removeAccount(account, null, null)
                    try {
                        accountManagerFuture.result
                    } catch (e: OperationCanceledException) {
                        CrashHandler.report(e)
                    } catch (e: AuthenticatorException) {
                        CrashHandler.report(e)
                    } catch (e: IOException) {
                        CrashHandler.report(e)
                    }
                }
            } else {
                Result.failure(Exception("Error while adding account"))
            })
        }

    @Parcelize
    data class LocalAccount(
        val id: Long,
        val label: String,
        val uuid: String,
        val isSynced: Boolean,
        val isSealed: Boolean
    ) : Parcelable

    private suspend fun buildResult(
        accountName: String,
        shouldReturnBackups: Boolean,
        shouldQueryLocalAccounts: Boolean,
        create: Boolean,
        shouldQueryRemoteAccounts: Boolean = true
    ): Result<SyncAccountData> {
        //noinspection Recycle
        val localAccounts = if (shouldQueryLocalAccounts) contentResolver.query(
            TransactionProvider.ACCOUNTS_URI,
            arrayOf(KEY_ROWID, KEY_LABEL, KEY_UUID, "$KEY_SYNC_ACCOUNT_NAME IS NULL", KEY_SEALED),
            null, null, null
        )?.useAndMapToList {
            val uuid = it.getString(2)
            if (uuid == null) {
                CrashHandler.report(Exception("Account with null uuid"))
                null
            } else {
                LocalAccount(
                    id = it.getLong(0),
                    label = it.getString(1),
                    uuid = uuid,
                    isSynced = it.getInt(3) == 0,
                    isSealed = it.getInt(4) == 1
                )
            }
        }?.filterNotNull() ?: emptyList() else emptyList()

        val account = getAccount(accountName)
        return SyncBackendProviderFactory.get(getApplication(), account, create)
            .mapCatching { syncBackendProvider ->
                val syncAccounts = if (shouldQueryRemoteAccounts)
                    syncBackendProvider.remoteAccountList
                        .mapNotNull { it.getOrNull() }
                        .filter { remoteAccount -> !localAccounts.any { it.isSynced && it.uuid == remoteAccount.uuid() } } else emptyList()
                val backups =
                    if (shouldReturnBackups) syncBackendProvider.storedBackups else emptyList()
                SyncAccountData(
                    accountName,
                    syncAccounts,
                    backups,
                    localAccounts.filter { !it.isSynced }
                )
            }.onFailure { throwable ->
            if (!(throwable is IOException || throwable is SyncBackendProvider.EncryptionException)) {
                SyncAdapter.log().e(throwable)
            }
        }
    }

    protected fun configureLocalAccountForSync(accountName: String, vararg uuids: String) {
        contentResolver.update(
            TransactionProvider.ACCOUNTS_URI,
            ContentValues().apply {
                put(KEY_SYNC_ACCOUNT_NAME, accountName)
            },
            KEY_UUID + " " + Operation.IN.getOp(uuids.size),
            uuids
        )
    }

    fun setupFromSyncAccounts(
        accountUuids: List<String>,
        accountName: String
    ): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            getSyncBackendProvider(
                getApplication<MyApplication>(),
                accountName
            ).onSuccess { syncBackendProvider ->
                emit(runCatching {
                    syncBackendProvider.remoteAccountList
                        .asSequence()
                        .mapNotNull { it.getOrNull() }
                        .filter { accountMetaData -> accountUuids.contains(accountMetaData.uuid()) }
                        .map { accountMetaData ->
                            accountMetaData.toAccount(
                                currencyContext.homeCurrencyString,
                                accountName
                            )
                        }
                        .forEach {
                            doSave(it)
                        }
                    GenericAccountService.activateSync(accountName, prefHandler)
                })
            }.onFailure {
                emit(Result.failure(it))
            }
        }


    fun fetchAccountData(accountName: String): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            emit(
                buildResult(
                    accountName,
                    shouldReturnBackups = true,
                    shouldQueryLocalAccounts = false,
                    create = false
                )
            )
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun removeBackend(accountName: String) =
        get(getApplication()).removeAccountExplicitly(getAccount(accountName))

    fun save(account: Account): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(kotlin.runCatching { doSave(account) })
        }

    companion object {
        const val KEY_ORIGINAL_ACCOUNT_NAME = "originalAccountName"
    }

    @Parcelize
    data class SyncAccountData(
        val accountName: String,
        val remoteAccounts: List<AccountMetaData>,
        val backups: List<String>,
        val localAccountsNotSynced: List<LocalAccount>
    ) : Parcelable
}