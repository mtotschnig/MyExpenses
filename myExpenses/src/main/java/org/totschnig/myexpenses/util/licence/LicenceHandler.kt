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
    fun hasValidKey() = true

    //called from PlayStoreLicenceHandler
    fun maybeUpgradeAddonFeatures(features: List<ContribFeature>, newPurchase: Boolean) {
        if (!hasOurLicence && !newPurchase) {
            addOnFeatures.clear()
        }
        addFeatures(features)
        persistAddonFeatures()
    }

    val hasAnyLicence: Boolean
        get() = true

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
        get() = true

    @get:VisibleForTesting
    val isExtendedEnabled: Boolean
        get() = true
    val isProfessionalEnabled: Boolean
        get() = true

    /**
     * @return user either has access through licence or through trial
     */
    fun hasTrialAccessTo(feature: ContribFeature) = true

    fun hasAccessTo(feature: ContribFeature) = true

    open fun isEnabledFor(licenceStatus: LicenceStatus) = true
