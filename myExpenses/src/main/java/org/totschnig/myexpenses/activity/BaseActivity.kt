package org.totschnig.myexpenses.activity

import android.app.DownloadManager
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import eltos.simpledialogfragment.form.AmountInputHostDialog
import icepick.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity.Companion.getIntentFor
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.DialogUtils.PasswordDialogUnlockedCallback
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.VersionDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.service.PlanExecutor.Companion.enqueueSelf
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.FeatureViewModel
import org.totschnig.myexpenses.viewmodel.OcrViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel
import org.totschnig.myexpenses.viewmodel.data.EventObserver
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY
import timber.log.Timber
import java.io.Serializable
import java.math.BigDecimal
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), MessageDialogFragment.MessageDialogListener,
    ConfirmationDialogListener, EasyPermissions.PermissionCallbacks, AmountInput.Host {
    private var snackBar: Snackbar? = null
    private var pwDialog: AlertDialog? = null

    private var _focusAfterRestoreInstanceState: Pair<Int, Int>? = null

    var scheduledRestart = false
    private var confirmCredentialResult: Boolean? = null

    override fun setFocusAfterRestoreInstanceState(focusView: Pair<Int, Int>?) {
        _focusAfterRestoreInstanceState = focusView
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        _focusAfterRestoreInstanceState?.let {
            findViewById<View>(it.first)?.findViewById<View>(it.second)?.requestFocus()
        }
    }

    val floatingActionButton: FloatingActionButton
        get() = _floatingActionButton!!

    private val _floatingActionButton: FloatingActionButton?
        get() = findViewById(R.id.CREATE_COMMAND)

    @JvmOverloads
    protected fun configureFloatingActionButton(fabDescription: Int, icon: Int = 0) {
        configureFloatingActionButton(getString(fabDescription))
        if (icon != 0) {
            floatingActionButton.setImageResource(icon)
        }
    }

    fun enqueuePlanner(forceImmediate: Boolean) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                enqueueSelf(this@BaseActivity, prefHandler, forceImmediate)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            enqueuePlanner(true)
        }
    }

    override fun showCalculator(amount: BigDecimal?, id: Int) {
        val intent = Intent(this, CalculatorInput::class.java).apply {
            forwardDataEntryFromWidget(this)
            if (amount != null) {
                putExtra(DatabaseConstants.KEY_AMOUNT, amount)
            }
            putExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, id)
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

    protected fun configureFloatingActionButton(fabDescription: String?) {
        floatingActionButton.contentDescription = fabDescription
    }

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onDownloadComplete()
        }
    }

    val progressDialogFragment: ProgressDialogFragment?
        get() = (supportFragmentManager.findFragmentByTag(LaunchActivity.PROGRESS_TAG) as? ProgressDialogFragment)

    fun copyToClipboard(text: String) {
        showSnackBar(
            try {
                ContextCompat.getSystemService(this, ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText(null, text))
                "${getString(R.string.toast_text_copied)}: $text"
            } catch (e: RuntimeException) {
                Timber.e(e)
                e.safeMessage
            }
        )
    }

    fun sendEmail(
        recipient: String,
        subject: String,
        body: String,
        forResultRequestCode: Int? = null
    ) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:$recipient"))
                },
                null
            ),
            R.string.no_app_handling_email_available,
            forResultRequestCode
        )
    }


    fun startActivity(intent: Intent, notAvailableMessage: Int, forResultRequestCode: Int? = null) {
        try {
            if (forResultRequestCode != null)
                startActivityForResult(intent, forResultRequestCode)
            else
                startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showSnackBar(notAvailableMessage)
        }
    }

    private fun onDownloadComplete() {
        downloadPending?.let {
            showSnackBar(getString(R.string.download_completed, it))
        }
        downloadPending = null
    }

    @State
    @JvmField
    var downloadPending: String? = null

    @Inject
    lateinit var prefHandler: PrefHandler

    val collate: String
        get() = prefHandler.collate

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var adHandlerFactory: AdHandlerFactory

    val ocrViewModel: OcrViewModel by viewModels()
    val featureViewModel: FeatureViewModel by viewModels()
    val shareViewModel: ShareViewModel by viewModels()

    private var helpVariant: Enum<*>? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        injectDependencies()
    }

    protected open fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    @CallSuper
    open fun onFeatureAvailable(feature: Feature) {
        featureManager.initActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        with((applicationContext as MyApplication).appComponent) {
            inject(ocrViewModel)
            inject(featureViewModel)
            inject(shareViewModel)
        }
        featureViewModel.getFeatureState().observe(this, EventObserver { featureState ->
            when (featureState) {
                is FeatureViewModel.FeatureState.FeatureLoading -> showSnackBar(
                    getString(
                        R.string.feature_download_requested,
                        getString(featureState.feature.labelResId)
                    )
                )
                is FeatureViewModel.FeatureState.FeatureAvailable -> {
                    Feature.values().find { featureState.modules.contains(it.moduleName) }?.let {
                        showSnackBar(
                            getString(
                                R.string.feature_downloaded,
                                getString(it.labelResId)
                            )
                        )
                        //after the dynamic feature module has been installed, we need to check if data needed by the module (e.g. Tesseract) has been downloaded
                        if (!featureViewModel.isFeatureAvailable(this, it)) {
                            featureViewModel.requestFeature(this, it)
                        } else {
                            onFeatureAvailable(it)
                        }
                    }
                }
                is FeatureViewModel.FeatureState.Error -> {
                    with(featureState.throwable) {
                        CrashHandler.report(this)
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
                    rebuildDbConstants()
                    recreate()
                }
            }
        })
        super.onCreate(savedInstanceState)
        tracker.init(this, licenceHandler.licenceStatus)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                shareViewModel.shareResult.collect { result ->
                    val callback = object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION) {
                                shareViewModel.messageShown()
                            }
                        }
                    }
                    result?.onFailure {
                        showDismissibleSnackBar(it.safeMessage, callback)
                    }?.onSuccess {
                        if (it == ShareViewModel.Scheme.HTTP || it == ShareViewModel.Scheme.HTTPS) {
                            showDismissibleSnackBar("HTTP PUT completed successfully.", callback)
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST) {
            if (resultCode == RESULT_OK) {
                confirmCredentialResult = true
                showWindow()
                requireApplication().isLocked = false
            } else {
                confirmCredentialResult = false
            }
        }
    }

    open fun requireApplication() = application as MyApplication

    override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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
                    confirmCredentials(CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST, null, true)
                }
            }
        }
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
        legacyUnlockCallback: PasswordDialogUnlockedCallback?,
        shouldHideWindow: Boolean
    ) {
        if (prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)) {
            val intent = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .createConfirmDeviceCredentialIntent(null, null)
            if (intent != null) {
                if (shouldHideWindow) hideWindow()
                try {
                    startActivityForResult(intent, requestCode)
                    requireApplication().isLocked = true
                } catch (e: ActivityNotFoundException) {
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
            requireApplication().isLocked = true
        }
    }

    open fun showDeviceLockScreenWarning() {
        showSnackBar(
            concatResStrings(
                this,
                " ",
                R.string.warning_device_lock_screen_not_set_up_1,
                R.string.warning_device_lock_screen_not_set_up_2
            )
        )
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
        } catch (e: IllegalArgumentException) {
            //Mainly hits Android 4, 5 and 6, no need to report
            //CrashHandler.report(e)
        }
        featureViewModel.unregisterCallback()
    }

    fun setTrackingEnabled(enabled: Boolean) {
        tracker.setEnabled(enabled)
    }

    fun logEvent(event: String, params: Bundle?) {
        tracker.logEvent(event, params)
    }

    fun trackCommand(command: Int) {
        try {
            resources.getResourceName(command)
        } catch (e: Resources.NotFoundException) {
            null
        }?.let { fullResourceName ->
            logEvent(Tracker.EVENT_DISPATCH_COMMAND, Bundle().apply {
                putString(
                    Tracker.EVENT_PARAM_ITEM_ID,
                    fullResourceName.substring(fullResourceName.indexOf('/') + 1)
                )
            })
        }
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
            else -> false
        }
    }

    fun processImageCaptureError(resultCode: Int, activityResult: CropImage.ActivityResult?) {
        if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            val throwable = activityResult?.error ?: Throwable("ERROR")
            CrashHandler.report(throwable)
            showSnackBar(if (throwable is ActivityNotFoundException) getString(R.string.image_capture_not_installed) else throwable.safeMessage)
        }
    }

    @JvmOverloads
    fun showDismissibleSnackBar(message: Int, callback: Snackbar.Callback? = null) {
        showDismissibleSnackBar(getText(message), callback)
    }

    @JvmOverloads
    fun showDismissibleSnackBar(
        message: CharSequence,
        callback: Snackbar.Callback? = null,
        actionLabel: String = getString(R.string.dialog_dismiss)
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
        callback: Snackbar.Callback? = null
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
        CrashHandler.report(Exception("Class $javaClass is unable to display snackBar"))
    }

    private val snackBarContainer: View?
        get() = findViewById(snackBarContainerId) ?: findViewById(android.R.id.content)

    fun showProgressSnackBar(
        message: CharSequence,
        total: Int = 0,
        progress: Int = 0,
        container: View? = null
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
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            snackBar = null
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
        message: CharSequence, duration: Int, snackBarAction: SnackbarAction?,
        callback: Snackbar.Callback?, container: View
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
        snackBar = null
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
                data = Uri.parse(uri)
            })
        } catch (e: ActivityNotFoundException) {
            showSnackBar("No activity found for opening $uri", Snackbar.LENGTH_LONG, null)
        }
    }

    @JvmOverloads
    open fun showMessage(
        message: CharSequence,
        positive: MessageDialogFragment.Button? = MessageDialogFragment.okButton(),
        neutral: MessageDialogFragment.Button? = null,
        negative: MessageDialogFragment.Button? = null,
        cancellable: Boolean = true
    ) {
        lifecycleScope.launchWhenResumed {
            MessageDialogFragment.newInstance(null, message, positive, neutral, negative).apply {
                isCancelable = cancellable
            }.show(supportFragmentManager, "MESSAGE")
        }
    }

    fun showVersionDialog(prev_version: Int) {
        lifecycleScope.launchWhenResumed {
            VersionDialogFragment.newInstance(prev_version)
                .show(supportFragmentManager, "VERSION_INFO")
        }
    }

    val unencryptedBackupWarning
        get() = getString(
            R.string.warning_unencrypted_backup,
            getString(R.string.pref_security_export_passphrase_title)
        )

    override fun onMessageDialogDismissOrCancel() {}

    fun rebuildDbConstants() {
        DatabaseConstants.buildLocalized(userLocaleProvider.getUserPreferredLocale())
        Transaction.buildProjection(this)
    }

    fun showMessage(resId: Int) {
        showMessage(getString(resId))
    }

    fun deleteFailureMessage(message: String?) =
        "There was an error deleting the object${message?.let { " ($it)" } ?: ""}. Please contact support@myexenses.mobi !"

    fun showDeleteFailureFeedback(message: String? = null, callback: Snackbar.Callback? = null) {
        showDismissibleSnackBar(deleteFailureMessage(message), callback)
    }

    protected open fun doHelp(variant: String?): Boolean {
        startActivity(Intent(this, Help::class.java).apply {
            putExtra(HelpDialogFragment.KEY_CONTEXT, helpContext)
            putExtra(HelpDialogFragment.KEY_VARIANT, variant ?: helpVariant?.name)
        })
        return true
    }

    protected open val helpContext: String
        get() = javaClass.simpleName

    fun setHelpVariant(helpVariant: Enum<*>, addBreadCrumb: Boolean = false) {
        this.helpVariant = helpVariant
        if (addBreadCrumb) {
            crashHandler.addBreadcrumb(helpVariant.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        _floatingActionButton?.let {
            it.isEnabled = true
        }
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    fun gdprConsent(personalized: Boolean) {
        adHandlerFactory.setConsent(this, personalized)
    }

    fun gdprNoConsent() {
        adHandlerFactory.clearConsent()
        contribFeatureRequested(ContribFeature.AD_FREE, null)
    }

    open fun contribFeatureRequested(feature: ContribFeature, tag: Serializable?) {
        if (licenceHandler.hasAccessTo(feature)) {
            (this as ContribIFace).contribFeatureCalled(feature, tag)
        } else {
            showContribDialog(feature, tag)
        }
    }

    open fun showContribDialog(feature: ContribFeature?, tag: Serializable?) {
        startActivityForResult(getIntentFor(this, feature).apply {
            putExtra(ContribInfoDialogActivity.KEY_TAG, tag)
        }, CONTRIB_REQUEST)
    }

    open fun requestPermission(permissionGroup: PermissionHelper.PermissionGroup) {
        _floatingActionButton?.let {
            it.isEnabled = false
        }
        EasyPermissions.requestPermissions(
            host = this,
            rationale = permissionGroup.permissionRequestRationale(this),
            requestCode = permissionGroup.requestCode,
            perms = permissionGroup.androidPermissions
        )
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(this)
                .title(R.string.permissions_label)
                .rationale(
                    PermissionHelper.PermissionGroup.fromRequestCode(requestCode)
                        .permissionRequestRationale(this)
                )
                .build().show()
        }
    }

    fun requireFeature(feature: Feature) {
        featureViewModel.requireFeature(this, feature)
    }

    fun isFeatureAvailable(feature: Feature) = featureViewModel.isFeatureAvailable(this, feature)

    override fun onNeutral(args: Bundle) {}
    override fun onNegative(args: Bundle) {}
    override fun onDismissOrCancel() {}
}