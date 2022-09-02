package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getSyncBackendProvider
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException
import java.lang.IllegalStateException

open class SyncViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    fun syncLinkRemote(account: Account): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            val accountId = Account.findByUuid(account.uuid)
            if (accountId == -1L) {
                emit(Result.failure(IllegalStateException("Account with uuid ${account.uuid} not found")))
            } else {
                emit(deleteAccountsInternal(arrayOf(accountId)).also {
                    it.onSuccess {
                        account.save()
                    }
                })
            }
        }

    fun syncLinkLocal(accountName: String, uuid: String): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            try {
                configureLocalAccountForSync(accountName, uuid)
            } catch (e: SQLiteConstraintException) {
                emit(Result.failure(AccountSealedException))
            }
            resetRemote(accountName, uuid)
            emit(ResultUnit)
        }


    fun resetRemote(accountName: String, uuid: String) {
        GenericAccountService.requestSync(accountName, uuid = uuid, extras = Bundle().apply {
            putBoolean(SyncAdapter.KEY_RESET_REMOTE_ACCOUNT, true)
        })
    }

    fun createSyncAccount(args: Bundle): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            val accountName = args.getString(AccountManager.KEY_ACCOUNT_NAME)!!
            val password = args.getString(AccountManager.KEY_PASSWORD)
            val userData = args.getBundle(AccountManager.KEY_USERDATA)
            val authToken = args.getString(AccountManager.KEY_AUTHTOKEN)
            val shouldReturnBackups = args.getBoolean(KEY_RETURN_BACKUPS)
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
                buildResult(accountName, shouldReturnBackups, true).onSuccess {
                    GenericAccountService.activateSync(account, prefHandler)
                }.onFailure {
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

    private fun buildResult(
        accountName: String,
        shouldReturnBackups: Boolean,
        create: Boolean
    ): Result<SyncAccountData> {
        val localAccounts = contentResolver.query(
            Account.CONTENT_URI,
            arrayOf(KEY_ROWID, KEY_LABEL, KEY_UUID, "$KEY_SYNC_ACCOUNT_NAME IS NULL", KEY_SEALED),
            null, null, null
        )?.use { cursor ->
            cursor.asSequence.mapNotNull {
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
            }.toList()
        } ?: emptyList()

        val account = getAccount(accountName)
        return SyncBackendProviderFactory[getApplication(), account, create].mapCatching { syncBackendProvider ->
            val syncAccounts =
                syncBackendProvider.remoteAccountList
                    .mapNotNull { it.getOrNull() }
                    .filter { remoteAccount -> !localAccounts.any { it.isSynced && it.uuid == remoteAccount.uuid() } }
            val backups =
                if (shouldReturnBackups) syncBackendProvider.storedBackups else emptyList()
            SyncAccountData(
                accountName,
                syncAccounts,
                backups,
                localAccounts.filter { !it.isSynced && !it.isSealed }
            )
        }.onFailure { throwable ->
            if (!(throwable is IOException || throwable is SyncBackendProvider.EncryptionException)) {
                SyncAdapter.log().e(throwable)
            }
        }
    }

    protected fun configureLocalAccountForSync(accountName: String, vararg uuids: String) {
        contentResolver.update(
            Account.CONTENT_URI,
            ContentValues().apply {
                put(KEY_SYNC_ACCOUNT_NAME, accountName)
            },
            KEY_UUID + " " + WhereFilter.Operation.IN.getOp(uuids.size),
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
                runCatching {
                    val numberOfRestoredAccounts =
                        syncBackendProvider.remoteAccountList
                            .asSequence()
                            .mapNotNull { it.getOrNull() }
                            .filter { accountMetaData -> accountUuids.contains(accountMetaData.uuid()) }
                            .map { accountMetaData ->
                                accountMetaData.toAccount(
                                    getApplication<MyApplication>().appComponent.currencyContext(),
                                    accountName
                                )
                            }
                            .sumOf {
                                @Suppress("USELESS_CAST")
                                (if (it.save() == null) 0 else 1) as Int
                            }
                    if (numberOfRestoredAccounts == 0) {
                        emit(Result.failure(Throwable("No accounts were restored")))
                    } else {
                        GenericAccountService.requestSync(accountName)
                        emit(ResultUnit)
                    }
                }
            }.onFailure {
                emit(Result.failure(it))
            }
        }


    fun fetchAccountData(accountName: String): LiveData<Result<SyncAccountData>> =
        liveData(context = coroutineContext()) {
            emit(buildResult(accountName, shouldReturnBackups = true, create = false))
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun removeBackend(accountName: String) =
        AccountManager.get(getApplication()).removeAccountExplicitly(getAccount(accountName))

    fun save(account: Account): LiveData<Uri?> =
        liveData(context = coroutineContext()) {
            emit(account.save())
        }

    companion object {
        const val KEY_RETURN_BACKUPS = "returnRemoteDataList"
    }

    @Parcelize
    data class SyncAccountData(
        val accountName: String,
        val remoteAccounts: List<AccountMetaData>,
        val backups: List<String>,
        val localAccountsNotSynced: List<LocalAccount>
    ) : Parcelable
}