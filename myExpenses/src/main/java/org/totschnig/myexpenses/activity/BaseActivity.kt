package org.totschnig.myexpenses.activity

import android.annotation.TargetApi
import android.app.DownloadManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.os.BundleCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import de.cketti.mailto.EmailIntentBuilder
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountInputHostDialog
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity.Companion.getIntentFor
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.databinding.ActivityWithFragmentBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.DialogUtils.PasswordDialogUnlockedCallback
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.TransactionDetailFragment
import org.totschnig.myexpenses.dialog.VersionDialogFragment
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.Module
import org.totschnig.myexpenses.feature.OcrHost
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.feature.values
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.service.PlanExecutor.Companion.enqueueSelf
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.AppDirHelper.ensureContentUri
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import org.totschnig.myexpenses.util.distrib.DistributionHelper.marketSelfUri
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.localizedQuote
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.util.ui.getAmountColor
import org.totschnig.myexpenses.util.ui.setBackgroundTintList
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel
import org.totschnig.myexpenses.viewmodel.FeatureViewModel
import org.totschnig.myexpenses.viewmodel.OcrViewModel
import org.totschnig.myexpenses.viewmodel.data.EventObserver
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sign
import androidx.core.net.toUri
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_NEGATIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_TAG_POSITIVE_BUNDLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH

abstract class BaseActivity : AppCompatActivity(), MessageDialogFragment.MessageDialogListener,
    ConfirmationDialogListener, EasyPermissions.PermissionCallbacks, AmountInput.Host, ContribIFace,
    OnDialogResultListener, OnSharedPreferenceChangeListener, OcrHost {
    private var snackBar: Snackbar? = null
    private var pwDialog: AlertDialog? = null

    private var _focusAfterRestoreInstanceState: Pair<Int, Int>? = null

    private var scheduledRestart = false
    private var confirmCredentialResult: Boolean? = null

    lateinit var toolbar: Toolbar

    open val fabActionName: String? = null

    override fun setFocusAfterRestoreInstanceState(focusView: Pair<Int, Int>?) {
        _focusAfterRestoreInstanceState = focusView
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        _focusAfterRestoreInstanceState?.let {
            findViewById<View>(it.first)?.findViewById<View>(it.second)?.requestFocus()
        }
    }

    lateinit var floatingActionButton: FloatingActionButton

    fun configureFloatingActionButton() {
        require(hasFloatingActionButton)
        with(floatingActionButton) {
            fabDescription?.let { contentDescription = getString(it) }
            fabIcon?.let { setImageResource(it) }
        }
    }

    private val hasFloatingActionButton: Boolean
        get() = ::floatingActionButton.isInitialized

    @StringRes
    open val fabDescription: Int? = null

    @DrawableRes
    open val fabIcon: Int? = null

    @JvmOverloads
    protected open fun setupToolbar(withHome: Boolean = true, homeAsUpIndicator: Int? = null) {
        toolbar = ActivityCompat.requireViewById<Toolbar>(this, R.id.toolbar).also {
            setSupportActionBar(it)
        }
        if (withHome) {
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                homeAsUpIndicator?.let {
                    setHomeAsUpIndicator(it)
                }
            }
        }
    }

    fun setSignedToolbarColor(amount: Long) {
        val sign = amount.sign
        toolbar.setSubtitleTextColor(getAmountColor(sign))
    }

    fun enqueuePlanner(forceImmediate: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                enqueueSelf(this@BaseActivity, prefHandler, forceImmediate)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR -> enqueuePlanner(true)
            PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI -> activateWebUi()
        }
    }

    override fun showCalculator(amount: BigDecimal?, id: Int) {
        val intent = Intent(this, CalculatorInput::class.java).apply {
            forwardDataEntryFromWidget(this)
            if (amount != null) {
                putExtra(DatabaseConstants.KEY_AMOUNT, amount.toString())
            }
            putExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, id)
            putExtra(KEY_COLOR, color)
        }
        (supportFragmentManager.findFragmentById(0) as? AmountInputHostDialog)?.also {
            it.startActivityForResult(intent, CALCULATOR_REQUEST)
        } ?: kotlin.run { startActivityForResult(intent, CALCULATOR_REQUEST) }
    }

    protected open fun forwardDataEntryFromWidget(intent: Intent) {
        intent.putExtra(
            EXTRA_START_FROM_WIDGET_DATA_ENTRY,
            getIntent().getBooleanExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, false)
        )
    }

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onDownloadComplete()
        }
    }

    val progressDialogFragment: ProgressDialogFragment?
        get() = (supportFragmentManager.findFragmentByTag(PROGRESS_TAG) as? ProgressDialogFragment)

    fun copyToClipboard(text: String) {
        showSnackBar(
            try {
                ContextCompat.getSystemService(this, ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText(null, text))
                "${getString(R.string.toast_text_copied)}: $text"
            } catch (e: RuntimeException) {
                report(e)
                e.safeMessage
            }
        )
    }

    fun sendEmail(
        recipient: String,
        subject: String,
        body: String,
    ) {
        if (!EmailIntentBuilder.from(this)
                .to(recipient)
                .subject(subject)
                .body(body)
                .start()
        ) {
            showMessage(body)
        }
    }

    fun startActivity(intent: Intent, notAvailableMessage: Int, forResultRequestCode: Int? = null) {
        try {
            if (forResultRequestCode != null)
                startActivityForResult(intent, forResultRequestCode)
            else
                startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showSnackBar(notAvailableMessage)
        } catch (e: SecurityException) {
            //seen on  Mate 20 Lite with cmp=com.simplemobiletools.calendar/.activities.MainActivity
            showSnackBar(e.safeMessage)
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        when (feature) {
            ContribFeature.BANKING -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.FINTS)) {
                    startBanking()
                } else {
                    featureViewModel.requestFeature(this, Feature.FINTS)
                }
            }

            ContribFeature.WEB_UI -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.WEBUI)) {
                    activateWebUi()
                } else {
                    featureViewModel.requestFeature(this, Feature.WEBUI)
                }
            }

            else -> {}
        }
    }

    fun activateWebUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) {
            requestNotificationPermission(PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI)
        } else {
            prefHandler.putBoolean(PrefKey.UI_WEB, true)
            onWebUiActivated()
        }
    }

    open fun onWebUiActivated() {}

    private fun onDownloadComplete() {
        downloadPending?.let {
            showSnackBar(getString(R.string.download_completed, it))
        }
        downloadPending = null
    }

    @State
    var downloadPending: String? = null

    @State
    var color = 0

    @Inject
    lateinit var prefHandler: PrefHandler

    val collate: String
        get() = prefHandler.collate

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var adHandlerFactory: AdHandlerFactory

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    val homeCurrency
        get() = currencyContext.homeCurrencyUnit

    val ocrViewModel: OcrViewModel by viewModels()
    val featureViewModel: FeatureViewModel by viewModels()
    val baseViewModel: BaseFunctionalityViewModel by viewModels()

    private var helpVariant: String? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        injectDependencies()
    }

    protected open fun injectDependencies() {
        injector.inject(this)
    }

    @CallSuper
    open fun onFeatureAvailable(feature: Feature) {
        featureManager.initActivity(this)
        when (feature) {
            Feature.FINTS -> startBanking()
            Feature.WEBUI -> activateWebUi()
            else -> {}
        }
    }

    fun harmonizeColors() {
        HarmonizedColors.applyToContextIfAvailable(
            this,
            HarmonizedColorsOptions.Builder()
                .setColorResourceIds(
                    intArrayOf(
                        R.color.colorExpenseLight,
                        R.color.colorIncomeLight,
                        R.color.colorExpenseDark,
                        R.color.colorIncomeDark,
                        R.color.UNRECONCILED,
                        R.color.CLEARED,
                        R.color.RECONCILED,
                        R.color.VOID
                    )
                )
                .build()
        )
    }

    private val contentColor: Int
        get() = if (canUseContentColor)
            color.takeIf { it != 0 } ?: intent.getIntExtra(KEY_COLOR, 0)
        else 0

    override fun onCreate(savedInstanceState: Bundle?) {
        if (uiConfigIsSafe) {
            enableEdgeToEdge()
        }
        with(injector) {
            inject(ocrViewModel)
            inject(featureViewModel)
            inject(baseViewModel)
        }

        StateSaver.restoreInstanceState(this, savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(
            this,
            DynamicColorsOptions.Builder().apply {
                contentColor.takeIf { it != 0 }?.let { setContentBasedSource(it) }
                if ("robolectric" != Build.FINGERPRINT) {
                    setOnAppliedCallback {
                        harmonizeColors()
                    }
                }
            }
                .build()
        )

        featureViewModel.getFeatureState().observe(this, EventObserver { featureState ->
            when (featureState) {
                is FeatureViewModel.FeatureState.FeatureLoading -> showSnackBar(
                    getString(
                        R.string.feature_download_requested,
                        getString(featureState.feature.labelResId)
                    )
                )

                is FeatureViewModel.FeatureState.FeatureAvailable -> {
                    showSnackBar(
                        featureState.modules.map { Module.print(this, it) }
                            .joinToString(" ") {
                                getString(R.string.feature_downloaded, it)
                            }
                    )
                    Feature.values.find { featureState.modules.contains(it.mainModule.moduleName) }
                        ?.also {
                            //after the dynamic feature module has been installed, we need to check if data needed by the module (e.g. Tesseract) has been downloaded
                            if (!featureViewModel.isFeatureAvailable(this, it)) {
                                featureViewModel.requestFeature(this, it)
                            } else {
                                onFeatureAvailable(it)
                            }
                        }
                        ?: run { report(Throwable("No feature found for ${featureState.modules.joinToString()}")) }

                }

                is FeatureViewModel.FeatureState.Error -> {
                    with(featureState.throwable) {
                        report(this)
                        message?.let { showSnackBar(it) }
                    }
                }

                is FeatureViewModel.FeatureState.LanguageLoading -> showSnackBar(
                    getString(
                        R.string.language_download_requested,
                        featureState.language
                    )
                )

                is FeatureViewModel.FeatureState.LanguageAvailable -> {
                    setLanguage(featureState.language)
                    recreate()
                }
            }
        })
        super.onCreate(savedInstanceState)
        tracker.init(this, licenceHandler.licenceStatus)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                baseViewModel.shareResult.collect { result ->
                    val callback = object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION) {
                                baseViewModel.messageShown()
                            }
                        }
                    }
                    result?.onFailure {
                        showDismissibleSnackBar(it.safeMessage, callback)
                    }?.onSuccess {
                        if (it == BaseFunctionalityViewModel.Scheme.HTTP || it == BaseFunctionalityViewModel.Scheme.HTTPS) {
                            showDismissibleSnackBar("HTTP PUT completed successfully.", callback)
                        }
                    }
                }
            }
        }
        if (prefHandler.shouldSecureWindow) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        handleRootWindowInsets()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (hasFloatingActionButton) {
            configureFloatingActionButton()
            floatingActionButton.setOnClickListener {
                onFabClicked()
            }
            ViewCompat.setOnApplyWindowInsetsListener(floatingActionButton) { v, windowInsets ->
                val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                val baseMargin = UiUtils.dp2Px(16f, resources)
                val insets =
                    if (imeVisible) Insets.NONE else windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.updateLayoutParams<MarginLayoutParams> {
                    leftMargin = baseMargin + insets.left
                    bottomMargin = baseMargin + insets.bottom
                    rightMargin = baseMargin + insets.right
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    @CallSuper
    open fun onFabClicked() {
        fabActionName?.let { trackCommand(it) }
    }

    fun setLanguage(language: String) {
        AppCompatDelegate.setApplicationLocales(
            if (language == MyApplication.DEFAULT_LANGUAGE)
                LocaleListCompat.getEmptyLocaleList() else
                LocaleListCompat.forLanguageTags(language)
        )

        //from Tiramisu on, change of language is handled in onConfigurationChange
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            currencyFormatter.invalidate(contentResolver)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    confirmCredentialResult = true
                    showWindow()
                    requireApplication().unlock()
                } else {
                    confirmCredentialResult = false
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    baseViewModel.cleanupOrigFile(result)
                    onCropResultOK(result)
                } else {
                    processImageCaptureError(resultCode, result)
                }
            }

            OCR_REQUEST -> ocrViewModel.handleOcrData(data, supportFragmentManager)
        }
    }

    open fun onCropResultOK(result: CropImage.ActivityResult) {
        ocrViewModel.startOcrFeature(result.uri, supportFragmentManager)
    }

    fun requireApplication() = application as MyApplication

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        featureViewModel.registerCallback()
        if (scheduledRestart) {
            scheduledRestart = false
            recreate()
        } else {
            confirmCredentialResult?.also {
                if (!it) {
                    moveTaskToBack(true)
                }
                confirmCredentialResult = null
            } ?: run {
                if (requireApplication().shouldLock(this)) {
                    confirmCredentials(CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST)
                }
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        when (dialogTag) {
            DIALOG_INACTIVE_BACKEND -> {
                if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                    GenericAccountService.activateSync(
                        extras.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)!!,
                        prefHandler
                    )
                }
                true
            }

            DIALOG_TAG_OCR_DISAMBIGUATE -> {
                startEditFromOcrResult(
                    BundleCompat.getParcelable(extras, KEY_OCR_RESULT, OcrResult::class.java)!!
                        .selectCandidates(
                            extras.getInt(DatabaseConstants.KEY_AMOUNT),
                            extras.getInt(DatabaseConstants.KEY_DATE),
                            extras.getInt(DatabaseConstants.KEY_PAYEE_NAME)
                        ),
                    BundleCompat.getParcelable(extras, KEY_URI, Uri::class.java)!!
                )
                true
            }

            else -> false
        }

    open fun hideWindow() {
        findViewById<View>(android.R.id.content).visibility = View.GONE
        supportActionBar?.hide()
    }

    open fun showWindow() {
        findViewById<View>(android.R.id.content).visibility = View.VISIBLE
        supportActionBar?.show()
    }

    protected open fun confirmCredentials(
        requestCode: Int,
        legacyUnlockCallback: PasswordDialogUnlockedCallback? = null,
        shouldHideWindow: Boolean = true,
        shouldLock: Boolean = true,
    ) {
        if (prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)) {
            val intent = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                if (shouldHideWindow) hideWindow()
                try {
                    startActivityForResult(intent, requestCode)
                    if (shouldLock) {
                        requireApplication().lock()
                    }
                } catch (_: ActivityNotFoundException) {
                    showSnackBar("No activity found for confirming device credentials")
                }
            } else {
                showDeviceLockScreenWarning()
                legacyUnlockCallback?.onPasswordDialogUnlocked()
            }
        } else if (prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, true)) {
            if (shouldHideWindow) hideWindow()
            if (pwDialog == null) {
                pwDialog = DialogUtils.passwordDialog(this, false)
            }
            DialogUtils.showPasswordDialog(this, pwDialog, legacyUnlockCallback)
            if (shouldLock) {
                requireApplication().lock()
            }
        }
    }

    open fun showDeviceLockScreenWarning() {
        showSnackBar(
            getString(R.string.warning_device_lock_screen_not_set_up_1) + " " +
                    getString(
                        R.string.warning_device_lock_screen_not_set_up_2,
                        localizedQuote(
                            concatResStrings(
                                this,
                                " -> ",
                                R.string.settings_label,
                                R.string.security_settings_title,
                                R.string.screen_lock
                            )
                        )
                    )
        )
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        if (key != null && prefHandler.matches(
                key,
                PrefKey.CUSTOM_DATE_FORMAT,
                PrefKey.DB_SAFE_MODE,
                PrefKey.GROUP_MONTH_STARTS,
                PrefKey.GROUP_WEEK_STARTS,
                PrefKey.HOME_CURRENCY,
                PrefKey.PROTECTION_ALLOW_SCREENSHOT,
                PrefKey.PROTECTION_DEVICE_LOCK_SCREEN,
                PrefKey.PROTECTION_LEGACY,
                PrefKey.UI_FONT_SIZE,
                PrefKey.CUSTOMIZE_MAIN_MENU,
                PrefKey.UI_ITEM_RENDERER_ORIGINAL_AMOUNT
            )
        ) {
            scheduledRestart = true
        }
    }

    override fun onPause() {
        super.onPause()
        val app = requireApplication()
        if (app.isLocked && pwDialog != null) {
            pwDialog!!.dismiss()
        } else {
            app.setLastPause(this)
        }
        try {
            unregisterReceiver(downloadReceiver)
        } catch (_: IllegalArgumentException) {
            //Mainly hits Android 4, 5 and 6, no need to report
            //CrashHandler.report(e)
        }
        featureViewModel.unregisterCallback()
    }

    fun logEvent(event: String, params: Bundle?) {
        tracker.logEvent(event, params)
    }

    fun trackCommand(command: Int) {
        try {
            resources.getResourceName(command)
        } catch (_: Resources.NotFoundException) {
            null
        }?.let { fullResourceName ->
            trackCommand(fullResourceName.substring(fullResourceName.indexOf('/') + 1))
        }
    }

    private fun trackCommand(command: String) {
        tracker.trackCommand(command)
    }

    @CallSuper
    override fun onPositive(args: Bundle, checked: Boolean) {
        val command = args.getInt(KEY_COMMAND_POSITIVE)
        dispatchCommand(
            command,
            args.getBundle(KEY_TAG_POSITIVE_BUNDLE)
        )
    }

    @CallSuper
    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        trackCommand(command)
        return when (command) {
            R.id.TESSERACT_DOWNLOAD_COMMAND -> {
                ocrViewModel.downloadTessData().observe(this) {
                    downloadPending = it
                }
                true
            }

            R.id.QUIT_COMMAND -> {
                finish()
                true
            }

            R.id.NOTIFICATION_SETTINGS_COMMAND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !PermissionGroup.NOTIFICATION.hasPermission(this)
                ) {
                    disableFab()
                    requestPermission(
                        PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_PLANNER,
                        PermissionGroup.NOTIFICATION
                    )
                } else {

                    val intent = Intent().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channelId = if (
                                NotificationManagerCompat.from(this@BaseActivity)
                                    .areNotificationsEnabled()
                            ) NotificationBuilderWrapper.CHANNEL_ID_PLANNER else null
                            action = when (channelId) {
                                null -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                else -> Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                            }
                            channelId?.let { putExtra(Settings.EXTRA_CHANNEL_ID, it) }
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        } else {
                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            putExtra("app_package", packageName)
                            putExtra("app_uid", applicationInfo.uid)
                        }
                    }
                    startActivity(intent)
                }
                true
            }

            R.id.RATE_COMMAND -> {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = marketSelfUri.toUri()
                }, R.string.error_accessing_market, null)
                true
            }

            R.id.SETTINGS_COMMAND -> {
                withResultCallbackLauncher.launch(PreferenceActivity.getIntent(this))
                true
            }

            R.id.FEEDBACK_COMMAND -> {
                val licenceStatus = licenceHandler.licenceStatus
                val licenceInfo = buildString {
                    if (licenceStatus != null) {
                        append(licenceStatus.name)
                    }
                    licenceHandler.purchaseExtraInfo.takeIf { !TextUtils.isEmpty(it) }?.let {
                        append(" ($it)")
                    }
                }.takeIf { it.isNotEmpty() }?.let {
                    "LICENCE: $it\n"
                }
                val crashHandlerInfo = crashHandler.getInfo()?.let {
                    "${it.first.uppercase(Locale.ROOT)}: ${it.second}\n"
                }
                val firstInstallVersion = prefHandler.getInt(PrefKey.FIRST_INSTALL_VERSION, 0)
                val firstInstallSchema =
                    prefHandler.getInt(PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION, -1)

                sendEmail(
                    recipient = getString(R.string.support_email),
                    subject = "[" + getString(R.string.app_name) + "] " + getString(R.string.feedback),
                    body = """
                        APP_VERSION:${getVersionInfo(this)}
                        FIRST_INSTALL_VERSION:$firstInstallVersion (DB_SCHEMA $firstInstallSchema)
                        ANDROID_VERSION:${Build.VERSION.RELEASE}
                        BRAND:${Build.BRAND}
                        MODEL:${Build.MODEL}
                        CONFIGURATION:${ConfigurationHelper.configToJson(resources.configuration)}
                        ${licenceInfo ?: ""}
                        ${crashHandlerInfo ?: ""}

                    """.trimIndent()
                )
                true
            }

            R.id.CONTRIB_INFO_COMMAND -> {
                showContribDialog(null, null)
                true
            }

            R.id.WEB_COMMAND -> {
                startActionView(getString(R.string.website))
                true
            }

            R.id.HELP_COMMAND -> doHelp(tag as String?)

            R.id.OPEN_PDF_COMMAND -> {
                startActionView((tag as String).toUri(), "application/pdf")
                true
            }

            R.id.SHARE_PDF_COMMAND -> {
                baseViewModel.share(
                    this, listOf(ensureContentUri((tag as String).toUri(), this)),
                    shareTarget,
                    "application/pdf"
                )
                true
            }

            android.R.id.home -> {
                doHome()
                true
            }

            R.id.FAQ_COMMAND -> {
                val path = (tag as? String) ?: (tag as? Bundle)?.getString(KEY_PATH)
                startActionView("https://faq.myexpenses.mobi/$path")
                true
            }

            else -> false
        }
    }

    val shareTarget: String
        get() = prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim()

    protected open fun doHome() {
        setResult(RESULT_CANCELED)
        finish()
    }

    open val imageCaptureErrorDismissCallback: Snackbar.Callback? = null

    open fun processImageCaptureError(resultCode: Int, activityResult: CropImage.ActivityResult?) =
        if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val throwable = activityResult?.error ?: Throwable("ERROR")
            report(throwable)
            showSnackBar(
                if (throwable is ActivityNotFoundException)
                    getString(R.string.image_capture_not_installed)
                else throwable.safeMessage,
                callback = imageCaptureErrorDismissCallback
            )
            true
        } else false

    @JvmOverloads
    fun showDismissibleSnackBar(message: Int, callback: Snackbar.Callback? = null) {
        showDismissibleSnackBar(getText(message), callback)
    }

    @JvmOverloads
    fun showDismissibleSnackBar(
        message: CharSequence,
        callback: Snackbar.Callback? = null,
        actionLabel: String = getString(R.string.dialog_dismiss),
    ) {
        showSnackBar(
            message, Snackbar.LENGTH_INDEFINITE,
            SnackbarAction(actionLabel) { snackBar?.dismiss() }, callback
        )
    }

    fun showSnackBarIndefinite(message: Int) {
        showSnackBar(message, Snackbar.LENGTH_INDEFINITE)
    }

    @JvmOverloads
    fun showSnackBar(message: Int, duration: Int = Snackbar.LENGTH_LONG) {
        showSnackBar(getText(message), duration)
    }

    @JvmOverloads
    fun showSnackBar(
        message: CharSequence,
        duration: Int = Snackbar.LENGTH_LONG,
        snackBarAction: SnackbarAction? = null,
        callback: Snackbar.Callback? = null,
    ) {
        snackBarContainer?.let {
            showSnackBar(message, duration, snackBarAction, callback, it)
        } ?: showSnackBarFallBack(message)
    }

    private fun showSnackBarFallBack(message: CharSequence) {
        reportMissingSnackBarContainer()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    open fun reportMissingSnackBarContainer() {
        report(Exception("Class $javaClass is unable to display snackBar"))
    }

    private val snackBarContainer: View?
        get() = findViewById(snackBarContainerId) ?: findViewById(android.R.id.content)

    fun showProgressSnackBar(
        message: CharSequence,
        total: Int = 0,
        progress: Int = 0,
        container: View? = null,
    ) {
        (container ?: snackBarContainer)?.also {
            val displayMessage = if (total > 0) "$message ($progress/$total)" else message
            if (snackBar == null) {
                snackBar = Snackbar.make(it, displayMessage, Snackbar.LENGTH_INDEFINITE).apply {
                    (view.findViewById<View>(com.google.android.material.R.id.snackbar_text).parent as ViewGroup)
                        .addView(
                            ProgressBar(
                                ContextThemeWrapper(
                                    this@BaseActivity,
                                    R.style.SnackBarTheme
                                )
                            ).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.CENTER_VERTICAL
                                }
                            }
                        )
                    addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                            if (snackBar == transientBottomBar) {
                                snackBar = null
                            }
                        }
                    })
                    show()
                }
            } else {
                snackBar?.setText(displayMessage)
            }
        } ?: showSnackBarFallBack(message)
    }

    fun updateDismissibleSnackBar(message: CharSequence) {
        snackBar?.setText(message) ?: run {
            showDismissibleSnackBar(message)
        }
    }

    fun showSnackBar(
        message: CharSequence,
        duration: Int,
        snackBarAction: SnackbarAction?,
        callback: Snackbar.Callback?,
        container: View,
    ) {
        snackBar = Snackbar.make(container, message, duration).apply {
            UiUtils.increaseSnackbarMaxLines(this)
            if (snackBarAction != null) {
                setAction(snackBarAction.label, snackBarAction.listener)
            }
            if (callback != null) {
                addCallback(callback)
            }
            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    snackBar = null
                }
            })
            show()
        }

    }

    fun dismissSnackBar() {
        snackBar?.dismiss()
    }

    @IdRes
    protected open val snackBarContainerId: Int = R.id.fragment_container

    private fun offerTessDataDownload() {
        ocrViewModel.offerTessDataDownload(this)
    }

    fun checkTessDataDownload() {
        ocrViewModel.tessDataExists().observe(this) {
            if (!it)
                offerTessDataDownload()
        }
    }

    fun startActionView(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = uri.toUri()
            })
        } catch (_: ActivityNotFoundException) {
            showSnackBar("No activity found for opening $uri")
        }
    }

    fun startActionView(uri: Uri, mimeType: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    ensureContentUri(uri, this@BaseActivity),
                    mimeType
                )
                setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            )
        } catch (_: ActivityNotFoundException) {
            showSnackBar(
                MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.uppercase(Locale.getDefault())
                    ?.let { getString(R.string.no_app_handling_mime_type_available, it) }
                    ?: "No activity found for opening $uri"
            )
        }
    }

    @JvmOverloads
    open fun showMessage(
        message: CharSequence,
        positive: MessageDialogFragment.Button? = MessageDialogFragment.okButton(),
        neutral: MessageDialogFragment.Button? = null,
        negative: MessageDialogFragment.Button? = null,
        cancellable: Boolean = true,
    ) {
        lifecycleScope.launchWhenResumed {
            MessageDialogFragment.newInstance(null, message, positive, neutral, negative).apply {
                isCancelable = cancellable
            }.show(supportFragmentManager, "MESSAGE")
        }
    }

    fun showVersionDialog(previousVersion: Int) {
        lifecycleScope.launchWhenResumed {
            VersionDialogFragment.newInstance(previousVersion)
                .show(supportFragmentManager, "VERSION_INFO")
        }
    }

    val unencryptedBackupWarning
        get() = getString(
            R.string.warning_unencrypted_backup,
            getString(R.string.pref_security_export_passphrase_title)
        )

    override fun onMessageDialogDismissOrCancel() {}

    fun initLocaleContext() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requireApplication().setUserPreferredLocale(AppCompatDelegate.getApplicationLocales()[0])
        }
        DatabaseConstants.buildLocalized(
            getLocale(),
            this,
            prefHandler
        )
    }

    fun deleteFailureMessage(message: String?) =
        "There was an error deleting the object${message?.let { " ($it)" } ?: ""}. Please contact support@myexenses.mobi !"

    fun showDeleteFailureFeedback(message: String? = null, callback: Snackbar.Callback? = null) {
        showDismissibleSnackBar(deleteFailureMessage(message), callback)
    }

    protected open fun doHelp(variant: String?): Boolean {
        startActivity(Intent(this, Help::class.java).apply {
            putExtra(HelpDialogFragment.KEY_CONTEXT, helpContext)
            putExtra(HelpDialogFragment.KEY_VARIANT, variant ?: helpVariant)
        })
        return true
    }

    protected open val helpContext: String
        get() = javaClass.simpleName

    fun setHelpVariant(helpVariant: String, addBreadCrumb: Boolean = false) {
        this.helpVariant = helpVariant
        if (addBreadCrumb) {
            crashHandler.addBreadcrumb(helpVariant)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasFloatingActionButton) {
            floatingActionButton.isEnabled = true
        }
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    open fun contribFeatureRequested(feature: ContribFeature, tag: Serializable? = null) {
        if (licenceHandler.hasAccessTo(feature)) {
            (this as ContribIFace).contribFeatureCalled(feature, tag)
        } else {
            showContribDialog(feature, tag)
        }
    }

    open fun showContribDialog(feature: ContribFeature? = null, tag: Serializable? = null) {
        startActivityForResult(getIntentFor(this, feature).apply {
            putExtra(ContribInfoDialogActivity.KEY_TAG, tag)
        }, CONTRIB_REQUEST)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission(requestCode: Int) {
        requestPermission(
            requestCode,
            PermissionGroup.NOTIFICATION
        )
    }

    fun checkNotificationPermissionForAutoBackup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionGroup.NOTIFICATION.hasPermission(this)
        ) {
            requestNotificationPermission(PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_AUTO_BACKUP)
        } else if (!areNotificationsEnabled(NotificationBuilderWrapper.CHANNEL_ID_AUTO_BACKUP)) {
            showSnackBar(
                TextUtils.concat(
                    getString(R.string.notifications_permission_required_auto_backup),
                    " ",
                    getString(
                        R.string.notifications_channel_required,
                        getString(R.string.pref_auto_backup_title)
                    )
                )
            )
        }
    }

    fun checkPermissionsForPlaner() {

        val missingPermissions = buildList {
            add(PermissionGroup.CALENDAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(PermissionGroup.NOTIFICATION)
            }
        }.filter { !it.hasPermission(this) }

        if (missingPermissions.contains(PermissionGroup.CALENDAR)) {
            disableFab()
            requestPermission(
                PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR,
                *missingPermissions.toTypedArray()
            )
        } else {

            val prefKey = "notification_permission_rationale_shown"
            if (!areNotificationsEnabled(NotificationBuilderWrapper.CHANNEL_ID_PLANNER) &&
                !prefHandler.getBoolean(prefKey, false)
            ) {
                ConfirmationDialogFragment.newInstance(
                    Bundle().apply {
                        putString(ConfirmationDialogFragment.KEY_PREFKEY, prefKey)
                        putCharSequence(
                            ConfirmationDialogFragment.KEY_MESSAGE,
                            TextUtils.concat(
                                Utils.getTextWithAppName(
                                    this@BaseActivity,
                                    R.string.notifications_permission_required_planner
                                ),
                                " ",
                                getString(
                                    R.string.notifications_channel_required,
                                    getString(R.string.planner_notification_channel_name)
                                )
                            )
                        )
                        putInt(
                            KEY_COMMAND_POSITIVE,
                            R.id.NOTIFICATION_SETTINGS_COMMAND
                        )
                        putInt(
                            KEY_POSITIVE_BUTTON_LABEL,
                            R.string.menu_reconfigure
                        )
                    }
                ).show(supportFragmentManager, "NOTIFICATION_PERMISSION_RATIONALE")
            }
        }
    }

    private fun areNotificationsEnabled(channelId: String) =
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel = manager.getNotificationChannel(channelId)
                channel?.importance != NotificationManager.IMPORTANCE_NONE
            } else {
                true
            }
        } else false

    fun requestCalendarPermission() {
        disableFab()
        requestPermission(
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR,
            PermissionGroup.CALENDAR
        )
    }

    private fun disableFab() {
        if (hasFloatingActionButton) {
            floatingActionButton.isEnabled = false
        }
    }

    open fun requestPermission(requestCode: Int, vararg permissionGroup: PermissionGroup) {
        EasyPermissions.requestPermissions(
            host = this,
            rationale = PermissionHelper.getRationale(this, requestCode, *permissionGroup),
            requestCode = requestCode,
            perms = permissionGroup.flatMap { it.androidPermissions }.toTypedArray()
        )
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(this)
                .title(R.string.permissions_label)
                .rationale(
                    PermissionHelper.getRationale(
                        this, requestCode,
                        *perms.map { PermissionGroup.fromPermission(it) }.distinct().toTypedArray()
                    )
                )
                .build().show()
        } else {
            showSnackBar(
                PermissionHelper.getRationale(
                    this, requestCode, PermissionGroup.NOTIFICATION
                )
            )
        }
    }

    fun showDetails(
        transactionId: Long,
        fullScreen: Boolean = false,
        currentFilter: FilterPersistence? = null,
        sortOrder: String? = null,
    ) {
        lifecycleScope.launchWhenResumed {
            TransactionDetailFragment.show(
                transactionId,
                supportFragmentManager,
                fullScreen,
                currentFilter,
                sortOrder
            )
        }
    }

    fun requireFeature(feature: Feature) {
        featureViewModel.requireFeature(this, feature)
    }

    fun isFeatureAvailable(feature: Feature) = featureViewModel.isFeatureAvailable(this, feature)

    override fun onNeutral(args: Bundle) {}
    override fun onNegative(args: Bundle) {}
    override fun onDismissOrCancel() {}

    fun hideKeyboard() {
        val im = applicationContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(window.decorView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun startMediaChooserDo(fileName: String) {
        lifecycleScope.launch {
            val uris = withContext(Dispatchers.IO) {
                PictureDirHelper.getOutputMediaUri(
                    temp = true,
                    application = myApplication,
                    fileName = fileName
                ) to PictureDirHelper.getOutputMediaUri(
                    temp = true,
                    application = myApplication,
                    fileName = "${fileName}_CROPPED"
                )
            }
            CropImage.activity()
                .setCameraOnly(!prefHandler.getBoolean(PrefKey.CAMERA_CHOOSER, false))
                .setCameraPackage(
                    prefHandler.getString(PrefKey.CAMERA_APP)?.takeIf { it.isNotEmpty() })
                .setAllowFlipping(false)
                .setCaptureImageOutputUri(uris.first)
                .setOutputUri(uris.second)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this@BaseActivity)
        }
    }

    fun maybeApplyDynamicColor(): Boolean = if (canUseContentColor) {
        val intent = getIntent() // Get the current intent
        intent.putExtra(KEY_IS_MANUAL_RECREATE, true)
        recreate()
        true
    } else false

    val canUseContentColor: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) uiConfigIsSafe else false
    }

    val uiConfigIsSafe: Boolean
        get() = if (prefHandler.getInt(PrefKey.UI_FONT_SIZE, 0) == 0) true else {
            val uiModeFromPref = prefHandler.uiMode(this)
            if (uiModeFromPref == "default") true else {
                val ourUiMode = if (uiModeFromPref == "dark")
                    UI_MODE_NIGHT_YES else UI_MODE_NIGHT_NO
                val systemUiMode =
                    applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                ourUiMode == systemUiMode
            }
        }

    fun tintFab(color: Int) {
        //If we use dynamic content based color, we do not need to harmonize the color
        val harmonized =
            if (canUseContentColor) color else MaterialColors.harmonizeWithPrimary(this, color)
        floatingActionButton.setBackgroundTintList(harmonized)
    }

    val withResultCallbackLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_RESTORE_OK) {
                restartAfterRestore()
            }
            if (it.resultCode == RESULT_INVALIDATE_OPTIONS_MENU) {
                invalidateOptionsMenu()
            }
        }

    protected val calledFromOnboarding: Boolean
        get() = callingActivity?.let {
            Utils.getSimpleClassNameFromComponentName(it)
        } == OnboardingActivity::class.java.simpleName

    val bankingFeature: BankingFeature
        get() = requireApplication().appComponent.bankingFeature() ?: BankingFeature

    protected open fun restartAfterRestore() {
        (application as MyApplication).invalidateHomeCurrency()
        if (!isFinishing) {
            finishAffinity()
            startActivity(Intent(this, MyExpenses::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
    }

    open fun startBanking() {
        startActivity(Intent(this, bankingFeature.bankingActivityClass))
    }

    fun requestSync(accountName: String, uuid: String? = null) {
        if (!GenericAccountService.requestSync(accountName, uuid = uuid)) {
            val bundle = Bundle(1)
            bundle.putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName)
            SimpleDialog.build()
                .msg(getString(R.string.warning_backend_deactivated))
                .pos(getString(R.string.button_activate_again))
                .extra(bundle)
                .show(this, DIALOG_INACTIVE_BACKEND)
        }
    }

    open fun checkGdprConsent(forceShow: Boolean) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                adHandlerFactory.gdprConsent(this@BaseActivity, forceShow)
            }
        }
    }

    val createAccountIntent
        get() = Intent(this, AccountEdit::class.java).apply {
            putExtra(KEY_COLOR, Account.DEFAULT_COLOR)
        }

    fun showTransferAccountMissingMessage() {
        showMessage(
            getString(R.string.dialog_command_disabled_insert_transfer),
            neutral = MessageDialogFragment.Button(
                R.string.menu_create_account,
                R.id.CREATE_ACCOUNT_FOR_TRANSFER_COMMAND,
                null,
                false
            )
        )
    }

    private fun displayDateCandidate(pair: Pair<LocalDate, LocalTime?>) =
        (pair.second?.let { pair.first.atTime(pair.second) } ?: pair.first).toString()

    override fun processOcrResult(result: Result<OcrResult>, scanUri: Uri) {

        result.onSuccess {
            if (it.needsDisambiguation()) {
                SimpleFormDialog.build()
                    .cancelable(false)
                    .autofocus(false)
                    .neg(android.R.string.cancel)
                    .extra(Bundle().apply {
                        putParcelable(KEY_OCR_RESULT, it)
                        putParcelable(KEY_URI, scanUri)
                    })
                    .title(getString(R.string.scan_result_multiple_candidates_dialog_title))
                    .fields(
                        when (it.amountCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_amount))
                            1 -> Hint.plain(
                                "${getString(R.string.amount)}: ${it.amountCandidates[0]}"
                            )

                            else -> Spinner.plain(DatabaseConstants.KEY_AMOUNT)
                                .placeholder(R.string.amount)
                                .items(*it.amountCandidates.toTypedArray())
                                .preset(0)
                        },
                        when (it.dateCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_date))
                            1 -> Hint.plain(
                                "${getString(R.string.date)}: ${it.dateCandidates[0]}"
                            )

                            else -> Spinner.plain(DatabaseConstants.KEY_DATE)
                                .placeholder(R.string.date)
                                .items(
                                    *it.dateCandidates.map(this::displayDateCandidate)
                                        .toTypedArray()
                                )
                                .preset(0)
                        },
                        when (it.payeeCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_payee))
                            1 -> Hint.plain(
                                "${getString(R.string.payee)}: ${it.payeeCandidates[0]}"
                            )

                            else -> Spinner.plain(DatabaseConstants.KEY_PAYEE_NAME)
                                .placeholder(R.string.payee)
                                .items(*it.payeeCandidates.map(Payee::name).toTypedArray())
                                .preset(0)
                        }
                    )
                    .show(this, DIALOG_TAG_OCR_DISAMBIGUATE)
            } else {
                startEditFromOcrResult(
                    it.selectCandidates(),
                    scanUri
                )
            }
        }.onFailure {
            report(it)
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    fun recordUsage(f: ContribFeature) {
        licenceHandler.recordUsage(f)
    }

    open fun startEditFromOcrResult(result: OcrResultFlat?, scanUri: Uri) {
        recordUsage(ContribFeature.OCR)
        lifecycleScope.launch {
            getEditIntent()?.apply {
                putExtra(KEY_OCR_RESULT, result)
                putExtra(KEY_URI, scanUri)
            }?.let { startEdit(it) }
        }
    }

    open suspend fun getEditIntent(): Intent? = Intent(this, ExpenseEdit::class.java)

    open fun startEdit(intent: Intent) {
        startActivityForResult(intent, EDIT_REQUEST)
    }

    protected fun setupWithFragment(
        doInstantiate: Boolean,
        withFab: Boolean = true,
        instantiate: (() -> Fragment),
    ) {
        ActivityWithFragmentBinding.inflate(layoutInflater).apply {
            if (doInstantiate) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        instantiate()
                    )
                    .commit()
            }
            if (withFab) {
                floatingActionButton = fab.CREATECOMMAND.also {
                    it.isVisible = true
                }
            }
            setContentView(root)
        }
    }

    open val scrollsHorizontally: Boolean = false

    open val drawToTopEdge: Boolean = false

    open val drawToBottomEdge: Boolean = true

    //We centrally deal with all window insets that should be consumed at the window root level
    //Only the bottom inset should be passed down to enable lists to scroll edge to edge
    open fun handleRootWindowInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        ViewGroupCompat.installCompatInsetsDispatch(rootView)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, receivedInsets ->
            // 1. Get the specific insets
            val imeInsets = receivedInsets.getInsets(WindowInsetsCompat.Type.ime())
            val displayCutoutInsets =
                receivedInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val systemBarsInsets = receivedInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarsInsets =
                receivedInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // 2. Determine the padding values to apply to this view 'v'
            val horizontalPaddingToApplyLeft = if (scrollsHorizontally) 0 else
                displayCutoutInsets.left.coerceAtLeast(systemBarsInsets.left)
            val horizontalPaddingToApplyRight = if (scrollsHorizontally) 0 else
                displayCutoutInsets.right.coerceAtLeast(systemBarsInsets.right)
            val topPaddingToApply = if (drawToTopEdge) 0 else
                displayCutoutInsets.top.coerceAtLeast(systemBarsInsets.top)

            val bottomPaddingToApply = imeInsets.bottom.coerceAtLeast(
                if (drawToBottomEdge) 0 else navigationBarsInsets.bottom
            )

            // 3. Apply padding to the current view 'v'
            v.updatePadding(
                left = horizontalPaddingToApplyLeft,
                top = topPaddingToApply,
                right = horizontalPaddingToApplyRight,
                bottom = bottomPaddingToApply
            )

            // 4. Construct new WindowInsetsCompat to return, indicating consumption
            val builder = WindowInsetsCompat.Builder(receivedInsets)

            // --- Consume HORIZONTAL parts of systemBars and displayCutout ---
            // We set the left and right insets for these types to 0 in what we pass down,
            // because 'v' has already applied this padding.

            // For System Bars:
            // Keep original bottom system bar insets, but zero out left/right
            builder.setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    if (scrollsHorizontally) systemBarsInsets.left else 0, // Left consumed by 'v'
                    if (drawToTopEdge) systemBarsInsets.top else 0, // Top consumed by 'v'),
                    if (scrollsHorizontally) systemBarsInsets.right else 0, // Right consumed by 'v'
                    systemBarsInsets.bottom // Bottom system bar inset is still available
                )
            )

            // For Display Cutout:
            // Keep original top and bottom display cutout insets, but zero out left/right
            builder.setInsets(
                WindowInsetsCompat.Type.displayCutout(),
                Insets.of(
                    if (scrollsHorizontally) displayCutoutInsets.left else 0, // Left consumed by 'v'
                    if (drawToTopEdge) displayCutoutInsets.top else 0, // Top consumed by 'v'),
                    if (scrollsHorizontally) displayCutoutInsets.right else 0, // Right consumed by 'v'
                    0
                )
            )

            // --- Handle IME consumption (bottom part) ---
            // If 'v' used the IME bottom inset, then the IME bottom inset should be marked as consumed.
            builder.setInsets(
                WindowInsetsCompat.Type.ime(),
                Insets.of(
                    imeInsets.left, // IME usually doesn't have horizontal insets, but preserve if they exist
                    imeInsets.top,
                    imeInsets.right,
                    0 // Bottom IME inset consumed by 'v'
                )
            )

            builder.setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(
                    navigationBarsInsets.left,
                    navigationBarsInsets.top,
                    navigationBarsInsets.right,
                    if (drawToBottomEdge) navigationBarsInsets.bottom else 0
                )
            )

            // --- Return the modified insets ---
            // Child views will receive these modified insets, where the horizontal system bars
            // and display cutout insets (and IME bottom) are now zeroed out,
            // preventing them from also applying padding for these consumed parts.
            builder.build()
        }
    }

    suspend fun StateFlow<Result<Pair<Uri, String>>?>.collectPrintResult(): Nothing =
        collect { result ->
            result?.let {
                dismissSnackBar()
                result.onSuccess { (uri, name) ->
                    recordUsage(ContribFeature.PRINT)
                    showMessage(
                        getString(R.string.export_sdcard_success, name),
                        MessageDialogFragment.Button(
                            R.string.menu_open,
                            R.id.OPEN_PDF_COMMAND,
                            uri.toString(),
                            true
                        ),
                        MessageDialogFragment.nullButton(R.string.button_label_close),
                        MessageDialogFragment.Button(
                            R.string.share,
                            R.id.SHARE_PDF_COMMAND,
                            uri.toString(),
                            true
                        ),
                        false
                    )
                }.onFailure {
                    report(it)
                    showSnackBar(it.safeMessage)
                }
                onPdfResultProcessed()
            }
        }

    open fun onPdfResultProcessed() {

    }

    fun showConfirmationDialog(
        tag: String,
        message: String,
        @IdRes commandPositive: Int,
        tagPositive: Bundle? = null,
        @StringRes commandPositiveLabel: Int = 0,
        @IdRes commandNegative: Int? = R.id.CANCEL_CALLBACK_COMMAND,
        prepareBundle: Bundle.() -> Unit = { },
    ) {
        ConfirmationDialogFragment
            .newInstance(Bundle().apply {
                putString(KEY_MESSAGE, message)
                putInt(KEY_COMMAND_POSITIVE, commandPositive)
                putInt(KEY_POSITIVE_BUTTON_LABEL, commandPositiveLabel)
                tagPositive?.let { putBundle(KEY_TAG_POSITIVE_BUNDLE, it) }
                commandNegative?.let { putInt(KEY_COMMAND_NEGATIVE, it) }
                prepareBundle()
            })
            .show(supportFragmentManager, tag)
    }


    companion object {
        const val ASYNC_TAG = "ASYNC_TASK"
        const val PROGRESS_TAG = "PROGRESS"
        private const val DIALOG_INACTIVE_BACKEND = "inactive_backend"
        const val RESULT_RESTORE_OK = RESULT_FIRST_USER + 1
        const val RESULT_INVALIDATE_OPTIONS_MENU = RESULT_FIRST_USER + 2
        const val KEY_IS_MANUAL_RECREATE = "IS_MANUAL_RECREATE"
    }
}