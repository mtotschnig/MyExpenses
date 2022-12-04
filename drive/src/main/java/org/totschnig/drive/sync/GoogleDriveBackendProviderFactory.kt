package org.totschnig.drive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.Keep
import org.totschnig.drive.activity.DriveSetup2
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import java.util.*

@Keep
class GoogleDriveBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = GoogleDriveBackendProvider(context, account, accountManager)

    override val setupActivityClass = DriveSetup2::class.java
}