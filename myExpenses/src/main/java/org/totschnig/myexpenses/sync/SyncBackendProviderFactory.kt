package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
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

    abstract fun startSetup(activity: ProtectedFragmentActivity)

    companion object {
        @JvmStatic
        fun getLegacy(
            context: Context,
            account: Account,
            create: Boolean
        ) = get(context, account, create).getOrThrow()

        @JvmStatic
        operator fun get(
            context: Context,
            account: Account,
            create: Boolean
        ): Result<SyncBackendProvider> {
            val accountManager = AccountManager.get(context)
            return BackendService.forAccount(account.name)
                ?.instantiate()
                ?.from(context, account, accountManager)
                ?.mapCatching {
                    it.setUp(
                        accountManager,
                        account,
                        loadPassword(context.contentResolver, account.name),
                        create
                    )
                    it
                } ?: Result.failure(SyncParseException("No Provider found for account $account"))
        }
    }
}