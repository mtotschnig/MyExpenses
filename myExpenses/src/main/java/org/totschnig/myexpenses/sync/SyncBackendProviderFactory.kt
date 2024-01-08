package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import arrow.core.flatMap
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.loadPassword
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException

abstract class SyncBackendProviderFactory {
    private fun from(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ): Result<SyncBackendProvider> = kotlin.runCatching {
        fromAccount(context, account, accountManager)
    }

    @Throws(SyncParseException::class)
    protected abstract fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ): SyncBackendProvider

    abstract val setupActivityClass: Class<out ProtectedFragmentActivity>

    companion object {
        const val ACTION_RECONFIGURE = "reconfigure"
        @JvmStatic
        suspend fun get(
            context: Context,
            account: Account,
            create: Boolean
        ): Result<SyncBackendProvider> {
            val accountManager = AccountManager.get(context)
            return BackendService.forAccount(account.name)
                .flatMap { it.instantiate() }
                .flatMap { it.from(context, account, accountManager) }
                .mapCatching {
                    it.setUp(
                        accountManager,
                        account,
                        loadPassword(context, account),
                        create
                    )
                    it
                }
        }
    }
}