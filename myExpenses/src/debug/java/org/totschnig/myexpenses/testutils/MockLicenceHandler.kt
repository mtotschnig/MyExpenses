package org.totschnig.myexpenses.testutils

import com.google.android.vending.licensing.PreferenceObfuscator
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceStatus
import java.time.Clock

class MockLicenceHandler(
    context: MyApplication,
    licenseStatusPrefs: PreferenceObfuscator,
    crashHandler: CrashHandler,
    prefHandler: PrefHandler,
    repository: Repository,
    currencyFormatter: ICurrencyFormatter,
    clock: Clock
) : LicenceHandler(context, licenseStatusPrefs, crashHandler, prefHandler, repository, currencyFormatter, clock) {
    fun setLockState(locked: Boolean) {
        this.licenceStatus = if (locked) null else LicenceStatus.PROFESSIONAL
        update()
    }
}