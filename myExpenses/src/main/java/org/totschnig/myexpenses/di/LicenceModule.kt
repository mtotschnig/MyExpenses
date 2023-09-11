package org.totschnig.myexpenses.di

import android.content.Context
import android.provider.Settings
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.Obfuscator
import com.google.android.vending.licensing.PreferenceObfuscator
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import javax.inject.Named
import javax.inject.Singleton

@Module
open class LicenceModule {
    @Provides
    @Singleton
    open fun providesLicenceHandler(
        preferenceObfuscator: PreferenceObfuscator,
        crashHandler: CrashHandler,
        application: MyApplication,
        prefHandler: PrefHandler,
        repository: Repository,
        currencyFormatter: ICurrencyFormatter
    ) = LicenceHandler(
        application,
        preferenceObfuscator,
        crashHandler,
        prefHandler,
        repository,
        currencyFormatter
    )

    @Provides
    @Singleton
    @Named("deviceId")
    open fun provideDeviceId(application: MyApplication): String =
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    @Provides
    @Singleton
    fun provideLicencePrefs(
        obfuscator: Obfuscator,
        application: MyApplication
    ): PreferenceObfuscator {
        val sp = application.getSharedPreferences("license_status_new", Context.MODE_PRIVATE)
        return PreferenceObfuscator(sp, obfuscator)
    }

    @Provides
    @Singleton
    open fun provideObfuscator(
        @Named("deviceId") deviceId: String,
        application: MyApplication
    ): Obfuscator = AESObfuscator(
        byteArrayOf(
            -1,
            -124,
            -4,
            -59,
            -52,
            1,
            -97,
            -32,
            38,
            59,
            64,
            13,
            45,
            -104,
            -3,
            -92,
            -56,
            -49,
            65,
            -25
        ), application.packageName, deviceId
    )
}