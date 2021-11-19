package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.Application
import android.content.ContentResolver
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.annimon.stream.Exceptional
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getSyncBackendProvider
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.asSequence
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException

class SyncViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun syncLinkRemote(account: Account): LiveData<Boolean> =
        liveData(context = coroutineContext()) {
            val accountId = Account.findByUuid(account.uuid)
            if (deleteAccountsInternal(arrayOf(accountId))) {
                account.save()
                emit(true)
            } else {
                emit(false)
            }
        }

    fun createSyncAccount(args: Bundle): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            val accountName = args.getString(AccountManager.KEY_ACCOUNT_NAME)!!
            val password = args.getString(AccountManager.KEY_PASSWORD)
            val userData = args.getBundle(AccountManager.KEY_USERDATA)
            val authToken = args.getString(AccountManager.KEY_AUTHTOKEN)
            val shouldReturnRemoteDataList =
                args.getBoolean(KEY_RETURN_REMOTE_DATA_LIST)
            val encryptionPassword = args.getString(GenericAccountService.KEY_PASSWORD_ENCRYPTION)
            val accountManager = AccountManager.get(getApplication())
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
                    contentResolver,
                    accountName,
                    encryptionPassword
                )
                buildResult(accountName, shouldReturnRemoteDataList, true).onSuccess {
                    GenericAccountService.activateSync(account, prefHandler)
                }.onFailure {
                    //we try to remove a failed account immediately, otherwise user would need to do it, before
                    //being able to try again
                    @Suppress("DEPRECATION") val accountManagerFuture = accountManager.removeAccount(account, null, null)
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

    private fun buildResult(
        accountName: String,
        shouldReturnRemoteDataList: Boolean,
        create: Boolean
    ): Result<SyncAccountData> {
        val localNotSynced = Account.count(
            DatabaseConstants.KEY_SYNC_ACCOUNT_NAME + " IS NULL", null
        )
        val account = getAccount(accountName)
        return SyncBackendProviderFactory[getApplication(), account, create].mapCatching { syncBackendProvider ->
            val syncAccounts =
                if (shouldReturnRemoteDataList) syncBackendProvider.remoteAccountStream
                    .filter { it.isPresent }
                    .map { it.get() }
                    .toList() else null
            val backups =
                if (shouldReturnRemoteDataList) syncBackendProvider.storedBackups else null
            SyncAccountData(
                accountName,
                syncAccounts,
                backups,
                localNotSynced
            )
        }.onFailure { throwable ->
            if (!(throwable is IOException || throwable is SyncBackendProvider.EncryptionException)) {
                SyncAdapter.log().e(throwable)
            }
        }
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
                runCatching {
                    val numberOfRestoredAccounts = syncBackendProvider.remoteAccountStream.asSequence()
                        .filter(Exceptional<AccountMetaData>::isPresent)
                        .map(Exceptional<AccountMetaData>::get)
                        .filter { accountMetaData -> accountUuids.contains(accountMetaData.uuid()) }
                        .map { accountMetaData -> accountMetaData.toAccount(getApplication<MyApplication>().appComponent.currencyContext()) }
                        .sumOf {
                            it.syncAccountName = accountName
                            @Suppress("USELESS_CAST")
                            (if (it.save() == null) 0  else 1) as Int
                        }
                    if (numberOfRestoredAccounts == 0) {
                        emit(Result.failure(Throwable("No accounts were restored")))
                    } else {
                        ContentResolver.requestSync(
                            getAccount(accountName),
                            TransactionProvider.AUTHORITY, Bundle().apply {
                                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                            }
                        )
                        emit(Result.success(Unit))
                    }
                }
            }.onFailure {
                emit(Result.failure(it))
            }
        }


    fun fetchAccountData(accountName: String): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            emit(buildResult(accountName, shouldReturnRemoteDataList = true, create = false))
        }

    companion object {
        const val KEY_RETURN_REMOTE_DATA_LIST = "returnRemoteDataList"
    }

    data class SyncAccountData(
        val accountName: String,
        val syncAccounts: List<AccountMetaData>?,
        val backups: List<String>?,
        val localNotSynced: Int
    )
}