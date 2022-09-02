package org.totschnig.myexpenses.activity

import android.app.DownloadManager
import android.content.*
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import icepick.State
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.VersionDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.FeatureViewModel
import org.totschnig.myexpenses.viewmodel.OcrViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel
import org.totschnig.myexpenses.viewmodel.data.EventObserver
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), MessageDialogFragment.MessageDialogListener, EasyPermissions.PermissionCallbacks {
    private var snackBar: Snackbar? = null
    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onDownloadComplete()
        }
    }

    val progressDialogFragment: ProgressDialogFragment?
        get() = (supportFragmentManager.findFragmentByTag(LaunchActivity.PROGRESS_TAG) as? ProgressDialogFragment)

    fun copyToClipboard(text: String) {
        showSnackBar(try {
            ContextCompat.getSystemService(this, ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
            "${getString(R.string.toast_text_copied)}: $text"
        } catch (e: RuntimeException) {
            Timber.e(e)
            e.safeMessage
        })
    }

    fun sendEmail(recipient: String, subject: String, body: String, forResultRequestCode: Int? = null) {
        startActivity(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:$recipient"))
        }, R.string.no_app_handling_email_available, forResultRequestCode)
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

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var crashHandler: CrashHandler

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

    open fun onFeatureAvailable(feature: Feature) {}

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
        tracker.init(this)
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

    override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        featureViewModel.registerCallback()
    }

    override fun onPause() {
        super.onPause()
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
        if (command == R.id.TESSERACT_DOWNLOAD_COMMAND) {
            ocrViewModel.downloadTessData().observe(this) {
                downloadPending = it
            }
            return true
        }
        return false
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
    fun showDismissibleSnackBar(message: CharSequence, callback: Snackbar.Callback? = null) {
        showSnackBar(
            message, Snackbar.LENGTH_INDEFINITE,
            SnackbarAction(R.string.dialog_dismiss) { snackBar?.dismiss() }, callback
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
        findViewById<View>(snackBarContainerId)?.let {
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

    fun showProgressSnackBar(message: CharSequence, total: Int = 0, progress: Int = 0) {
        findViewById<View>(snackBarContainerId)?.let {
            val displayMessage = if (total > 0) "$message ($progress/$total)" else message
            if (progress > 0) {
                snackBar?.setText(displayMessage)
            } else {
                snackBar = Snackbar.make(it, displayMessage, Snackbar.LENGTH_INDEFINITE).apply {
                    (view.findViewById<View>(com.google.android.material.R.id.snackbar_text).parent as ViewGroup)
                        .addView(
                            ProgressBar(
                                ContextThemeWrapper(
                                    this@BaseActivity,
                                    R.style.SnackBarTheme
                                )
                            )
                        )
                    show()
                }
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
                setAction(snackBarAction.resId, snackBarAction.listener)
            }
            if (callback != null) {
                addCallback(callback)
            }
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
        positive: MessageDialogFragment.Button = MessageDialogFragment.okButton(),
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

    fun showVersionDialog(prev_version: Int, showImportantUpgradeInfo: ArrayList<Int>) {
        lifecycleScope.launchWhenResumed {
            VersionDialogFragment.newInstance(prev_version, showImportantUpgradeInfo)
                .show(supportFragmentManager, "VERSION_INFO")
        }
    }

    fun unencryptedBackupWarning() = getString(
        R.string.warning_unencrypted_backup,
        getString(R.string.pref_security_export_passphrase_title)
    )

    override fun onMessageDialogDismissOrCancel() {}

    fun rebuildDbConstants() {
        DatabaseConstants.buildLocalized(userLocaleProvider.getUserPreferredLocale())
        Transaction.buildProjection(this)
        rebuildAccountProjection()
    }

    fun rebuildAccountProjection() {
        Account.buildProjection()
    }

    fun showMessage(resId: Int) {
        showMessage(getString(resId))
    }

    fun showDeleteFailureFeedback(message: String? = null, callback: Snackbar.Callback? = null) {
        showDismissibleSnackBar("There was an error deleting the object${message?.let { " ($it)" } ?: ""}. Please contact support@myexenses.mobi !", callback)
    }

    fun getHelpVariant() = helpVariant?.name

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
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    open fun requestPermission(permissionGroup: PermissionHelper.PermissionGroup) {
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
                .rationale(PermissionHelper.PermissionGroup.fromRequestCode(requestCode).permissionRequestRationale(this))
                .build().show()
        }
    }

    fun requireFeature(feature: Feature) {
        featureViewModel.requireFeature(this, feature)
    }

    fun isFeatureAvailable(feature: Feature) = featureViewModel.isFeatureAvailable(this, feature)
}