package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import timber.log.Timber
import javax.inject.Singleton

@Module
open class FeatureModule {
    private var ocrFeature: OcrFeature? = null
    private var bankingFeature: BankingFeature? = null

    @Provides
    fun provideOcrFeature(prefHandler: PrefHandler): OcrFeature? = ocrFeature ?:
        try {
            (Class.forName("org.totschnig.fints.OcrFeatureImpl").getConstructor(PrefHandler::class.java)
                .newInstance(prefHandler) as OcrFeature).also {
                ocrFeature = it
            }
        } catch (e: ClassNotFoundException) {
            null
        }

    @Provides
    fun provideBankingFeature(): BankingFeature? = bankingFeature ?:
    try {
        (Class.forName("org.totschnig.fints.BankingFeatureImpl").newInstance() as BankingFeature).also {
            bankingFeature = it
        }
    } catch (e: ClassNotFoundException) {
        null
    }

    @Provides
    @Singleton
    open fun provideFeatureManager(prefHandler: PrefHandler): FeatureManager = try {
        Class.forName("org.totschnig.myexpenses.util.locale.PlatformSplitManager")
                .getConstructor(PrefHandler::class.java)
                .newInstance(prefHandler) as FeatureManager
    } catch (e: Exception) {
        if (DistributionHelper.distribution != DistributionHelper.Distribution.GITHUB) {
            Timber.e(e)
        }
        object : FeatureManager() {}
    }
}