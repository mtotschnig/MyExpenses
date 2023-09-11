package org.totschnig.myexpenses.testutils

import com.google.android.vending.licensing.Obfuscator
import com.google.android.vending.licensing.PreferenceObfuscator
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.di.LicenceModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.time.Clock

class MockLicenceModule(private val clock: Clock) : LicenceModule() {
    override fun provideDeviceId(application: MyApplication) = "DUMMY"

    override fun provideObfuscator(deviceId: String, application: MyApplication) =
        object : Obfuscator {
            override fun obfuscate(original: String, key: String): String {
                return original
            }

            override fun unobfuscate(obfuscated: String, key: String): String {
                return obfuscated
            }
        }

    override fun providesLicenceHandler(
        preferenceObfuscator: PreferenceObfuscator,
        crashHandler: CrashHandler,
        application: MyApplication,
        prefHandler: PrefHandler,
        repository: Repository,
        currencyFormatter: ICurrencyFormatter
    ): LicenceHandler = MockLicenceHandler(application, preferenceObfuscator, crashHandler, prefHandler, repository, currencyFormatter, clock)
}