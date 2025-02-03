package org.totschnig.myexpenses.util.licence

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.google.android.vending.licensing.PreferenceObfuscator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.IapActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.EU_COUNTRIES
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import timber.log.Timber
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

open class LicenceHandler(
    protected val context: Application,
    var licenseStatusPrefs: PreferenceObfuscator,
    private val crashHandler: CrashHandler,
    protected val prefHandler: PrefHandler,
    private val repository: Repository,
    private val currencyFormatter: ICurrencyFormatter,
    private val clock: Clock = Clock.systemUTC(),
) {
    private var hasOurLicence = false
    private val isSandbox = BuildConfig.DEBUG

    class LicenceUpdateEvent

    private val licenceStatusFlow by lazy {
        MutableStateFlow(LicenceUpdateEvent())
    }

    protected suspend fun licenceStatusUpdated() {
        licenceStatusFlow.emit(LicenceUpdateEvent())
    }

    var licenceStatus: LicenceStatus? = null
        internal set(value) {
            crashHandler.putCustomData("Licence", value?.name ?: "null")
            field = value
        }
    protected val addOnFeatures: MutableSet<ContribFeature> = mutableSetOf()

    val currencyUnit: CurrencyUnit = CurrencyUnit("EUR", "€", 2)
    fun hasValidKey() = hasOurLicence

    //called from PlayStoreLicenceHandler
    fun maybeUpgradeAddonFeatures(features: List<ContribFeature>, newPurchase: Boolean) {
        if (!hasOurLicence && !newPurchase) {
            addOnFeatures.clear()
        }
        addFeatures(features)
        persistAddonFeatures()
    }

    val hasAnyLicence: Boolean
        get() = licenceStatus != null || addOnFeatures.isNotEmpty()

    private fun addFeatures(features: List<ContribFeature>) {
        addOnFeatures.addAll(features)
    }

    //called from PlayStoreLicenceHandler
    fun maybeUpgradeLicence(licenceStatus: LicenceStatus?) {
        //we downgrade only if we do not have our own licence
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
    fun hasTrialAccessTo(feature: ContribFeature) =
        hasAccessTo(feature) || usagesLeft(feature)

    fun hasAccessTo(feature: ContribFeature) =
        isEnabledFor(feature.licenceStatus) || addOnFeatures.contains(feature)

    open fun isEnabledFor(licenceStatus: LicenceStatus) =
        (this.licenceStatus?.compareTo(licenceStatus) ?: -1) >= 0

    val isUpgradeable: Boolean
        get() = licenceStatus?.isUpgradeable != false

    open fun init() {
        this.licenceStatus = enumValueOrNull<LicenceStatus>(
            licenseStatusPrefs.getString(LICENSE_STATUS_KEY, null)
        )?.also {
            hasOurLicence = true
        }
        restoreAddOnFeatures()
    }

    fun update() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Template.updateNewPlanEnabled()
                updateNewAccountEnabled()
            } catch (_: Exception) {
            }
            GenericAccountService.updateAccountsIsSyncable(
                context,
                this@LicenceHandler,
                prefHandler
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutHelper.configureSplitShortcut(context, isContribEnabled)
            }
        }
    }

    open suspend fun voidLicenceStatus(keepFeatures: Boolean) {
        licenseStatusPrefs.remove(LICENSE_STATUS_KEY)
        licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY)
        licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY)
        if (!keepFeatures) {
            addOnFeatures.clear()
            licenseStatusPrefs.remove(LICENSE_FEATURES)
        }
        if (addOnFeatures.isEmpty()) {
            hasOurLicence = false
        }
        licenseStatusPrefs.commit()
        licenceStatusUpdated()
        this.licenceStatus = null
    }

    open suspend fun updateLicenceStatus(licence: Licence) {
        hasOurLicence = true
        val licence = licence.let {
            if (it.type == LicenceStatus.EXTENDED && it.fallback)
                it.copy(type = LicenceStatus.EXTENDED_FALLBACK)
            else it
        }
        licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licence.type?.name ?: "null")
        if (licence.validSince != null) {
            val validSince =
                licence.validSince.atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT-14"))
            licenseStatusPrefs.putString(
                LICENSE_VALID_SINCE_KEY,
                (validSince.toEpochSecond() * 1000).toString()
            )
        }
        if (licence.validUntil != null) {
            val validUntil =
                licence.validUntil.atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT+12"))
            licenseStatusPrefs.putString(
                LICENSE_VALID_UNTIL_KEY,
                (validUntil.toEpochSecond() * 1000).toString()
            )
        } else {
            licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY)
        }
        addFeatures(licence.featureList)
        persistAddonFeatures()
        licenseStatusPrefs.commit()
        this.licenceStatus = licence.type
        licenceStatusUpdated()
        update()
    }

    fun reset() {
        init()
        update()
    }

    open fun getFormattedPrice(aPackage: Package): String? {
        return getFormattedPriceWithExtra(aPackage, false)
    }

    @Suppress("RedundantNullableReturnType")
    fun getFormattedPriceWithExtra(aPackage: Package, withExtra: Boolean): String? {
        return aPackage.getFormattedPrice(
            context,
            currencyFormatter,
            currencyUnit,
            withExtra,
            usesSubscriptions
        )
    }

    fun getFormattedPriceWithSaving(aPackage: ProfessionalPackage): String? {
        val withExtra = licenceStatus === LicenceStatus.EXTENDED
        val formattedPrice = getFormattedPriceWithExtra(aPackage, withExtra)
        val base = ProfessionalPackage.Professional_6
        return if (aPackage == base) formattedPrice else String.format(
            Locale.ROOT, "%s (- %d %%)", formattedPrice,
            100 - aPackage.defaultPrice * 100 * base.getDuration(withExtra) /
                    (aPackage.getDuration(withExtra) * base.defaultPrice)
        )
    }

    open fun getExtendOrSwitchMessage(aPackage: ProfessionalPackage): String {
        val extendedDate = DateUtils.addMonths(
            Date(validUntilMillis.coerceAtLeast(clock.millis())),
            aPackage.getDuration(false)
        )
        return context.getString(
            R.string.extend_until,
            Utils.getDateFormatSafe(context).format(extendedDate),
            aPackage.getFormattedPriceRaw(currencyUnit, currencyFormatter)
        )
    }

    open fun getProLicenceStatus(context: Context) = getProValidUntil(context)

    @Suppress("MemberVisibilityCanBePrivate") //used from Huawei
    fun getProValidUntil(context: Context): String? {
        return validUntilMillis.takeIf { it != 0L }?.let {
            context.getString(
                R.string.valid_until,
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .format(epochMillis2LocalDate(it))
            )
        }
    }

    val validUntilMillis: Long
        get() = licenseStatusPrefs.getString(LICENSE_VALID_UNTIL_KEY, "0").toLong()

    @Suppress("MemberVisibilityCanBePrivate")
    val validSinceMillis: Long
        get() = licenseStatusPrefs.getString(LICENSE_VALID_SINCE_KEY, "0").toLong()

    open val proPackages: Array<ProfessionalPackage>
        get() = arrayOf(
            ProfessionalPackage.Professional_6,
            ProfessionalPackage.Professional_12,
            ProfessionalPackage.Professional_24
        )

    open fun getExtendedUpgradeGoodyMessage(selectedPackage: ProfessionalPackage): String? {
        return context.getString(R.string.extended_upgrade_goodie_github, 3)
    }

    open val professionalPriceShortInfo: String
        get() = joinPriceInformation(*proPackages)

    @Suppress("MemberVisibilityCanBePrivate") //used from Amazon
    protected fun joinPriceInformation(vararg packages: Package) =
        packages.mapNotNull(::getFormattedPrice)
            .joinToString(" ${context.getString(R.string.joining_or)} ")

    open val proPackagesForExtendOrSwitch: Array<ProfessionalPackage>?
        get() = proPackages

    open fun getProLicenceAction(context: Context): String {
        return context.getString(R.string.extend_validity)
    }

    open val purchaseExtraInfo: String?
        get() = null

    open val roadmapVoteKey: Pair<String, String>?
        get() = if (isProfessionalEnabled)
            prefHandler.getString(PrefKey.NEW_LICENCE)?.let {
                DistributionHelper.Distribution.GITHUB.name to it
            }
        else null

    /**
     * @return true if licenceStatus has been upEd
     */
    open fun registerUnlockLegacy(): Boolean {
        return false
    }

    fun getPaymentOptions(aPackage: Package, userCountry: String) = listOfNotNull(
        R.string.donate_button_paypal,
        if (aPackage.defaultPrice >= 500 && EU_COUNTRIES.contains(userCountry))
            R.string.donate_button_invoice else null
    )

    open val needsKeyEntry: Boolean
        get() = true

    open val usesSubscriptions: Boolean
        get() = false

    fun getPaypalUri(aPackage: Package): String {
        val host = if (isSandbox) "www.sandbox.paypal.com" else "www.paypal.com"
        var uri = "https://$host/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=${
            aPackage.payPalButtonId(isSandbox)
        }&on0=${aPackage.optionName}&os0=${aPackage::class.java.simpleName}&lc=$paypalLocale&currency_code=EUR"
        prefHandler.getString(PrefKey.LICENCE_EMAIL, null)?.let {
            uri += "&custom=" + Uri.encode(it)
        }
        Timber.d("Paypal URI: %s", uri)
        return uri
    }

    val backendUri = when {
        isSandbox -> "http://10.0.2.2:3000/"
        else -> "https://licencedb.myexpenses.mobi/"
    }

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

    suspend fun handleExpiration() {
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
        addOnFeatures.takeIf { it.isNotEmpty() }
            ?.joinToString { context.getString(it.labelResId) }?.let {
                if (result == null) {
                    result = ""
                } else {
                    result += " "
                }
                result += "(+ $it)"
            }
        return result
    }

    fun getButtonLabel(aPackage: Package): String {
        val resId = when (aPackage) {
            Package.Contrib -> LicenceStatus.CONTRIB.resId
            Package.Upgrade -> R.string.pref_contrib_purchase_title_upgrade
            Package.Extended -> LicenceStatus.EXTENDED.resId
            is ProfessionalPackage -> LicenceStatus.PROFESSIONAL.resId
            is AddOnPackage -> aPackage.feature.labelResId
        }
        return String.format(
            "%s (%s)",
            context.getString(resId),
            getFormattedPriceWithExtra(aPackage, licenceStatus === LicenceStatus.EXTENDED)
        )
    }

    open fun initBillingManager(activity: IapActivity, query: Boolean): BillingManager? {
        return null
    }

    open suspend fun launchPurchase(
        aPackage: Package,
        shouldReplaceExisting: Boolean,
        billingManager: BillingManager,
    ) {
    }

    private fun persistAddonFeatures() {
        val joinToString = addOnFeatures.joinToString(",", transform = ContribFeature::name)
        licenseStatusPrefs.putString(LICENSE_FEATURES, joinToString)
        crashHandler.putCustomData("AddOns", joinToString)
    }

    private fun restoreAddOnFeatures() {
        licenseStatusPrefs.getString(LICENSE_FEATURES, null)?.split(',')?.mapNotNull {
            try {
                ContribFeature.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }?.let { addFeatures(it) }
    }

    fun updateNewAccountEnabled() {
        val newAccountEnabled =
            hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED) ||
                    repository.countAccounts() < ContribFeature.FREE_ACCOUNTS
        prefHandler.putBoolean(PrefKey.NEW_ACCOUNT_ENABLED, newAccountEnabled)
    }

    fun recordUsage(feature: ContribFeature) {
        if (!hasAccessTo(feature) &&
            feature.trialMode == ContribFeature.TrialMode.DURATION &&
            !prefHandler.isSet(feature.prefKey)
        ) {
            prefHandler.putLong(feature.prefKey, clock.millis())
        }
    }


    fun usagesLeft(feature: ContribFeature) = when (feature.trialMode) {
        ContribFeature.TrialMode.DURATION -> getEndOfTrial(feature) > clock.millis()
        ContribFeature.TrialMode.UNLIMITED -> true
        else -> false
    }

    private fun getStartOfTrial(feature: ContribFeature): Long {
        return prefHandler.getLong(feature.prefKey, clock.millis())
    }

    fun getEndOfTrial(feature: ContribFeature) =
        getStartOfTrial(feature) + TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS)

    @Composable
    fun ManageLicence(
        contribBuyDo: (ProfessionalPackage?) -> Unit,
        validateLicence: () -> Unit,
        removeLicence: () -> Unit,
        manageSubscription: (Uri) -> Unit,
    ) {
        AppTheme {
            key(licenceStatusFlow.collectAsState().value) {
                if (hasAnyLicence) {
                    val isPro = licenceStatus == LicenceStatus.PROFESSIONAL
                    Column(
                        modifier = Modifier.padding(
                            dimensionResource(id = R.dimen.general_padding)
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.thank_you),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (hasOurLicence) {
                            Text(
                                prefHandler.getString(
                                    PrefKey.LICENCE_EMAIL,
                                    ""
                                ) + ": " + prefHandler.getString(PrefKey.NEW_LICENCE, "")
                            )
                        }
                        Text(prettyPrintStatus(context)!!)
                        if (isPro) {
                            getProLicenceStatus(context)?.let { Text(it) }
                            proPackagesForExtendOrSwitch?.forEach {
                                Button(onClick = { contribBuyDo(it) }) {
                                    Text(getExtendOrSwitchMessage(it))
                                }
                            }
                        } else {
                            Button(onClick = { contribBuyDo(null) }) {
                                Text(
                                    stringResource(
                                        id = R.string.pref_contrib_purchase_title_upgrade
                                    )
                                )
                            }
                        }
                        if (needsKeyEntry) {
                            Button(onClick = validateLicence) {
                                Text(stringResource(id = R.string.button_validate))
                            }
                            Button(onClick = removeLicence) {
                                Text(stringResource(id = R.string.remove))
                            }
                        }
                        subscriptionManagementLink?.let {
                            Button(onClick = {
                                manageSubscription(it)
                            }) {
                                Text(stringResource(id = R.string.pref_category_title_manage))
                            }
                        }
                    }
                } else {
                    Button(
                        modifier = Modifier.wrapContentSize(),
                        onClick = { contribBuyDo(null) }) {
                        Text(stringResource(id = R.string.menu_contrib))
                    }
                }
            }
        }
    }

    open val subscriptionManagementLink: Uri?
        get() = null


    companion object {
        protected const val LICENSE_STATUS_KEY = "licence_status"
        protected const val LICENSE_FEATURES = "licence_features"
        private const val LICENSE_VALID_SINCE_KEY = "licence_valid_since"
        private const val LICENSE_VALID_UNTIL_KEY = "licence_valid_until"
        const val TAG = "LicenceHandler"
        const val TRIAL_DURATION_DAYS = 60L
        fun log(): Timber.Tree {
            return Timber.tag(TAG)
        }
    }
}