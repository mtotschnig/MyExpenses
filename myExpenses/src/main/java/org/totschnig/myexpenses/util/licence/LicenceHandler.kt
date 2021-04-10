package org.totschnig.myexpenses.util.licence

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.google.android.vending.licensing.PreferenceObfuscator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class LicenceHandler(protected val context: MyApplication, var licenseStatusPrefs: PreferenceObfuscator, private val crashHandler: CrashHandler, protected val prefHandler: PrefHandler) {
    private var hasOurLicence = false
    private val isSandbox = BuildConfig.DEBUG
    private val localBackend = false
    var licenceStatus: LicenceStatus? = null
        @VisibleForTesting set(value) {
            crashHandler.putCustomData("Licence", value?.name ?: "null")
            field = value
        }
    val addOnFeatures: MutableSet<ContribFeature> = mutableSetOf()

    val currencyUnit: CurrencyUnit = CurrencyUnit("EUR", "€", 2)
    fun hasValidKey(): Boolean {
        return hasOurLicence
    }

    fun maybeUpgradeAddonFeatures(features: List<ContribFeature>, newPurchase: Boolean) {
        if (!hasOurLicence && !newPurchase) {
            addOnFeatures.clear()
        }
        addFeatures(features)
    }

    private fun addFeatures(features: List<ContribFeature>) {
        addOnFeatures.addAll(features)
        persistAddonFeatures()
    }

    fun maybeUpgradeLicence(licenceStatus: LicenceStatus?) {
        if (!hasOurLicence || this.licenceStatus?.greaterOrEqual(licenceStatus) != true) {
            this.licenceStatus = licenceStatus
        }
    }

    val isContribEnabled: Boolean
        get() = isEnabledFor(LicenceStatus.CONTRIB)

    @get:VisibleForTesting
    val isExtendedEnabled: Boolean
        get() = isEnabledFor(LicenceStatus.EXTENDED)
    val isProfessionalEnabled: Boolean
        get() = isEnabledFor(LicenceStatus.PROFESSIONAL)

    /**
     * @return user either has access through licence or through trial
     */
    fun hasTrialAccessTo(feature: ContribFeature): Boolean {
        return hasAccessTo(feature) || feature.usagesLeft(prefHandler) > 0
    }

    fun hasAccessTo(feature: ContribFeature): Boolean {
        return isEnabledFor(feature.licenceStatus) || addOnFeatures.contains(feature)
    }

    open fun isEnabledFor(licenceStatus: LicenceStatus) =
            this.licenceStatus?.compareTo(licenceStatus) ?: -1 >= 0

    val isUpgradeable: Boolean
        get() = licenceStatus?.isUpgradeable ?: false

    open fun init() {
        this.licenceStatus = licenseStatusPrefs.getString(LICENSE_STATUS_KEY, null)?.let {
            hasOurLicence = true
            try {
                LicenceStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        restoreAddOnFeatures()
    }

    fun update() {
        CoroutineScope(Dispatchers.IO).launch {
            Template.updateNewPlanEnabled()
            Account.updateNewAccountEnabled()
            GenericAccountService.updateAccountsIsSyncable(context, this@LicenceHandler, prefHandler)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutHelper.configureSplitShortcut(context, isContribEnabled)
            }
        }
    }

    open fun voidLicenceStatus(keepFeatures: Boolean) {
        this.licenceStatus = null
        licenseStatusPrefs.remove(LICENSE_STATUS_KEY)
        licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY)
        licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY)
        if (!keepFeatures) {
            addOnFeatures.clear()
            licenseStatusPrefs.remove(LICENSE_FEATURES)
        }
        licenseStatusPrefs.commit()
    }

    open fun updateLicenceStatus(licence: Licence) {
        hasOurLicence = true
        this.licenceStatus = licence.type
        licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licence.type?.name ?: "null")
        addFeatures(licence.featureList)
        if (licence.validSince != null) {
            val validSince = licence.validSince.atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT-14"))
            licenseStatusPrefs.putString(LICENSE_VALID_SINCE_KEY, (validSince.toEpochSecond() * 1000).toString())
        }
        if (licence.validUntil != null) {
            val validUntil = licence.validUntil.atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT+12"))
            licenseStatusPrefs.putString(LICENSE_VALID_UNTIL_KEY, (validUntil.toEpochSecond() * 1000).toString())
        } else {
            licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY)
        }
        licenseStatusPrefs.commit()
        update()
    }

    fun reset() {
        init()
        update()
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun setLockState(locked: Boolean) {
        this.licenceStatus = if (locked) null else LicenceStatus.PROFESSIONAL
        update()
    }

    open fun getFormattedPrice(aPackage: Package): String? {
        return getFormattedPriceWithExtra(aPackage, false)
    }

    @Suppress("RedundantNullableReturnType")
    fun getFormattedPriceWithExtra(aPackage: Package, withExtra: Boolean): String? {
        return aPackage.getFormattedPrice(context, currencyUnit, withExtra)
    }

    fun getFormattedPriceWithSaving(aPackage: ProfessionalPackage): String? {
        val withExtra = licenceStatus === LicenceStatus.EXTENDED
        val formattedPrice = getFormattedPriceWithExtra(aPackage, withExtra)
        val base = ProfessionalPackage.Professional_6
        return if (aPackage == base) formattedPrice else String.format(Locale.ROOT, "%s (- %d %%)", formattedPrice,
                100 - aPackage.defaultPrice * 100 * base.getDuration(withExtra) /
                        (aPackage.getDuration(withExtra) * base.defaultPrice))
    }

    open fun getExtendOrSwitchMessage(aPackage: ProfessionalPackage): String {
        val extendedDate = DateUtils.addMonths(
                Date(validUntilMillis.coerceAtLeast(System.currentTimeMillis())),
                aPackage.getDuration(false))
        return context.getString(R.string.extend_until,
                Utils.getDateFormatSafe(context).format(extendedDate),
                aPackage.getFormattedPriceRaw(currencyUnit, context))
    }

    open fun getProLicenceStatus(context: Context) = getProValidUntil(context)

    @Suppress("MemberVisibilityCanBePrivate") //used from Huawei
    fun getProValidUntil(context: Context): String? {
        return validUntilMillis.takeIf { it != 0L }?.let { context.getString(R.string.valid_until, Utils.getDateFormatSafe(this.context).format(Date(it))) }
    }

    val validUntilMillis: Long
        get() = licenseStatusPrefs.getString(LICENSE_VALID_UNTIL_KEY, "0").toLong()

    @Suppress("MemberVisibilityCanBePrivate")
    val validSinceMillis: Long
        get() = licenseStatusPrefs.getString(LICENSE_VALID_SINCE_KEY, "0").toLong()

    open val proPackages: Array<ProfessionalPackage>
        get() = arrayOf(ProfessionalPackage.Professional_6, ProfessionalPackage.Professional_12, ProfessionalPackage.Professional_24)

    open fun getExtendedUpgradeGoodyMessage(selectedPackage: ProfessionalPackage): String? {
        return context.getString(R.string.extended_upgrade_goodie_github, 3)
    }

    open val professionalPriceShortInfo: String
        get() = joinPriceInformation(*proPackages)

    @Suppress("MemberVisibilityCanBePrivate") //used from Amazon
    protected fun joinPriceInformation(vararg packages: Package) =
            packages.map(::getFormattedPrice).joinToString(" ${context.getString(R.string.joining_or)} ")

    open val proPackagesForExtendOrSwitch: Array<ProfessionalPackage>?
        get() = proPackages

    open fun getProLicenceAction(context: Context): String {
        return context.getString(R.string.extend_validity)
    }

    open val purchaseExtraInfo: String?
        get() = null

    open fun buildRoadmapVoteKey(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * @return true if licenceStatus has been upEd
     */
    open fun registerUnlockLegacy(): Boolean {
        return false
    }

    fun getPaymentOptions(aPackage: Package): IntArray {
        return if (aPackage.defaultPrice >= 500) intArrayOf(R.string.donate_button_paypal, R.string.donate_button_invoice) else intArrayOf(R.string.donate_button_paypal)
    }

    open val doesUseIAP: Boolean
        get() = false

    open val needsKeyEntry: Boolean
        get() = true

    fun getPaypalUri(aPackage: Package): String {
        val host = if (isSandbox) "www.sandbox.paypal.com" else "www.paypal.com"
        var uri = String.format(Locale.US,
                "https://%s/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=%s&on0=%s&os0=%s&lc=%s&currency_code=EUR",
                host, aPackage.payPalButtonId(isSandbox), aPackage.optionName, aPackage::class.java.simpleName, paypalLocale)
        prefHandler.getString(PrefKey.LICENCE_EMAIL, null)?.let {
            uri += "&custom=" + Uri.encode(it)
        }
        Timber.d("Paypal URI: %s", uri)
        return uri
    }

    val backendUri: String
        get() =
            if (isSandbox)
                if (localBackend)
                    "http://10.0.2.2:3000/"
                else
                    "https://myexpenses-licencedb-staging.herokuapp.com"
            else
                "https://licencedb.myexpenses.mobi/"

    private val paypalLocale: String
        get() {
            val locale = Locale.getDefault()
            return when (locale.language) {
                "en" -> "en_US"
                "fr" -> "fr_FR"
                "es" -> "es_ES"
                "zh" -> "zh_CN"
                "ar" -> "ar_EG"
                "de" -> "de_DE"
                "nl" -> "nl_NL"
                "pt" -> "pt_PT"
                "da" -> "da_DK"
                "ru" -> "ru_RU"
                "id" -> "id_ID"
                "iw", "he" -> "he_IL"
                "it" -> "it_IT"
                "ja" -> "ja_JP"
                "no" -> "no_NO"
                "pl" -> "pl_PL"
                "ko" -> "ko_KO"
                "sv" -> "sv_SE"
                "th" -> "th_TH"
                else -> "en_US"
            }
        }

    fun handleExpiration() {
        val licenceDuration = validUntilMillis - validSinceMillis
        if (TimeUnit.MILLISECONDS.toDays(licenceDuration) > 240) { // roughly eight months
            licenceStatus = LicenceStatus.EXTENDED_FALLBACK
            licenseStatusPrefs.putString(LICENSE_STATUS_KEY, LicenceStatus.EXTENDED_FALLBACK.name)
            licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY)
            licenseStatusPrefs.commit()
        } else {
            voidLicenceStatus(true)
        }
    }

    fun prettyPrintStatus(context: Context): String? {
        var result = licenceStatus?.let { context.getString(it.resId) }
        addOnFeatures.takeIf { it.isNotEmpty() }?.joinToString { context.getString(it.getLabelResIdOrThrow(context)) }?.let {
            if (result == null) {
                result = ""
            } else {
                result += " "
            }
            result += "(+ $it)"
        }
        if (licenceStatus == LicenceStatus.PROFESSIONAL) {
            getProLicenceStatus(context)?.let {
                result += String.format(" (%s)", it)
            }
        }
        return result
    }

    fun getButtonLabel(aPackage: Package): String {
        val resId = when (aPackage) {
            Package.Contrib -> LicenceStatus.CONTRIB.resId
            Package.Upgrade -> R.string.pref_contrib_purchase_title_upgrade
            Package.Extended -> LicenceStatus.EXTENDED.resId
            is ProfessionalPackage -> LicenceStatus.PROFESSIONAL.resId
            is AddOnPackage -> aPackage.feature.getLabelResIdOrThrow(context)
        }
        return String.format("%s (%s)", context.getString(resId), getFormattedPriceWithExtra(aPackage, licenceStatus === LicenceStatus.EXTENDED))
    }

    open fun initBillingManager(activity: Activity, query: Boolean): BillingManager? {
        return null
    }

    open fun launchPurchase(aPackage: Package, shouldReplaceExisting: Boolean, billingManager: BillingManager) {}

    private fun persistAddonFeatures() {
        val joinToString = addOnFeatures.joinToString(",", transform = ContribFeature::name)
        licenseStatusPrefs.putString(LICENSE_FEATURES, joinToString)
        crashHandler.putCustomData("AddOns", joinToString)
    }

    private fun restoreAddOnFeatures() {
        licenseStatusPrefs.getString(LICENSE_FEATURES, null)?.split(',')?.mapNotNull {
            try {
                ContribFeature.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }?.let { addFeatures(it) }
    }

    open fun supportSingleFeaturePurchase(feature: ContribFeature) = feature.licenceStatus == LicenceStatus.PROFESSIONAL

    companion object {
        protected const val LICENSE_STATUS_KEY = "licence_status"
        protected const val LICENSE_FEATURES = "licence_features"
        private const val LICENSE_VALID_SINCE_KEY = "licence_valid_since"
        private const val LICENSE_VALID_UNTIL_KEY = "licence_valid_until"
        const val TAG = "LicenceHandler"
        fun log(): Timber.Tree {
            return Timber.tag(TAG)
        }
    }
}