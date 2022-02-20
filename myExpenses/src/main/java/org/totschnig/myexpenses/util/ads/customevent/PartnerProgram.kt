package org.totschnig.myexpenses.util.ads.customevent

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.annotation.ArrayRes
import androidx.core.util.Pair
import org.totschnig.myexpenses.preference.PrefHandler
import timber.log.Timber
import java.util.*

/**
 * @param adSizes: List should be sorted by width then height, since this order is expected by [.pickContentResId]
 */
enum class PartnerProgram(
    private val distributionCountries: List<String>,
    private val adSizes: List<MyAdSize>
) {
    ;

    @Suppress("unused")
    enum class MyAdSize(val width: Int, @Suppress("unused") val height: Int) {
        SMALL(200, 50), BANNER(320, 50), FULL_BANNER(468, 60), LEADERBOARD(728, 90);

    }

    fun shouldShowIn(country: String, prefHandler: PrefHandler) =
        distributionCountries.contains(country) &&
                System.currentTimeMillis() -
                prefHandler.getLong(prefKey, 0) > DateUtils.HOUR_IN_MILLIS * 4

    private val prefKey: String
        get() = PREFERENCE_PREFIX + name

    @ArrayRes
    fun pickContentResId(context: Context, availableWidth: Int): Int {
        return adSizes.asSequence().filter { value: MyAdSize -> availableWidth >= value.width }
            .onEach { myAdSize: MyAdSize? -> Timber.d("%s", myAdSize) }
            .map { adSize: MyAdSize ->
                val name = CONTENT_RES_PREFIX + name + "_" + adSize.name
                Timber.d(name)
                context.resources.getIdentifier(name, "array", context.packageName)
            }
            .filter { resId: Int -> resId != 0 }
            .firstOrNull() ?: 0
    }

    @ArrayRes
    fun pickContentInterstitial(context: Context): Int {
        val orientation = context.resources.configuration.orientation
        val name =
            CONTENT_RES_PREFIX + name + "_" + if (orientation == Configuration.ORIENTATION_PORTRAIT) "PORTRAIT" else "LANDSCAPE"
        return context.resources.getIdentifier(name, "array", context.packageName)
    }

    fun record(prefHandler: PrefHandler) {
        prefHandler.putLong(prefKey, System.currentTimeMillis())
    }

    companion object {
        private const val CONTENT_RES_PREFIX = "custom_ads_html_"
        private const val PREFERENCE_PREFIX = "custom_ads_last_shown_"
        fun pickContent(
            partnerPrograms: List<PartnerProgram>,
            userCountry: String,
            prefHandler: PrefHandler,
            context: Context,
            availableWidth: Int
        ): Pair<PartnerProgram, String>? {
            val forInterstitial = availableWidth == -1
            val contentProviders = partnerPrograms
                .filter { partnerProgram: PartnerProgram ->
                    partnerProgram.shouldShowIn(
                        userCountry,
                        prefHandler
                    )
                }
                .map { partnerProgram: PartnerProgram ->
                    Pair.create(
                        partnerProgram,
                        if (forInterstitial) partnerProgram.pickContentInterstitial(context) else partnerProgram.pickContentResId(
                            context,
                            availableWidth
                        )
                    )
                }
                .filter { pair: Pair<PartnerProgram, Int> -> pair.second != 0 }
            val nrOfProviders = contentProviders.size
            if (nrOfProviders > 0) {
                val random = Random()
                val contentProvider: Pair<PartnerProgram, Int> = if (nrOfProviders == 1) {
                    contentProviders[0]
                } else {
                    contentProviders[random.nextInt(nrOfProviders)]
                }
                val adContent = context.resources.getStringArray(contentProvider.second)
                return Pair.create(contentProvider.first, adContent[random.nextInt(adContent.size)])
            }
            return null
        }
    }
}