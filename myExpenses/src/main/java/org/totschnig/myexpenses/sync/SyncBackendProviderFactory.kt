package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.loadPassword
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.util.asExceptional
import java.io.Serializable

abstract class SyncBackendProviderFactory {
    private fun from(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ): Result<SyncBackendProvider>? {
        return if (account.name.startsWith(label)) {
            kotlin.runCatching {
                fromAccount(context, account, accountManager)
            }
        } else null
    }

    fun buildAccountName(extra: String): String {
        return "$label - $extra"
    }

    @Throws(SyncParseException::class)
    protected abstract fun fromAccount(
        context: Context?,
        account: Account?,
        accountManager: AccountManager?
    ): SyncBackendProvider

    abstract val label: String
    abstract fun startSetup(activity: ProtectedFragmentActivity?)

    @SuppressWarnings("unused")
    fun isEnabled(context: Context?): Boolean {
        return true
    }

    abstract val id: Int
    abstract fun getRepairIntent(activity: Activity?): Intent?
    abstract fun startRepairTask(activity: ManageSyncBackends?, data: Intent?): Boolean
    abstract fun handleRepairTask(mExtra: Serializable?): org.totschnig.myexpenses.util.Result<*>?
    abstract fun init()

    companion object {
        @JvmStatic
        fun getLegacy(
            context: Context,
            account: Account,
            create: Boolean
        ) = get(context, account, create).asExceptional()

        @JvmStatic
        operator fun get(
            context: Context,
            account: Account,
            create: Boolean
        ): Result<SyncBackendProvider> {
            val accountManager = AccountManager.get(context)
            return ServiceLoader.load(context).mapNotNull {
                    it.from(context, account, accountManager)
                }
                .firstOrNull()?.mapCatching {
                    val result = it.setUp(
                        accountManager.blockingGetAuthToken(
                            account,
                            GenericAccountService.AUTH_TOKEN_TYPE, true
                        ),
                        loadPassword(context.contentResolver, account.name), create
                    )
                    if (!result.isPresent) {
                        throw result.exception
                    }
                    it
                } ?: Result.failure(SyncParseException("No Provider found for account $account"))
        }
    }
}