package org.totschnig.myexpenses.activity

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.VersionDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.FeatureViewModel
import org.totschnig.myexpenses.viewmodel.OcrViewModel
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), MessageDialogFragment.MessageDialogListener {
    private var snackbar: Snackbar? = null
    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onDownloadComplete()
        }
    }

    private fun onDownloadComplete() {
        downloadPending?.let {
            showSnackbar(getString(R.string.download_completed, it))
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

    lateinit var ocrViewModel: OcrViewModel
    lateinit var featureViewModel: FeatureViewModel

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        injectDependencies()
    }

    protected open fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    open fun onFeatureAvailable(feature : Feature) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        ocrViewModel = ViewModelProvider(this).get(OcrViewModel::class.java)
        featureViewModel = ViewModelProvider(this).get(FeatureViewModel::class.java)
        featureViewModel.getFeatureState().observe(this, { featureState ->
            when (featureState) {
                is FeatureViewModel.FeatureState.Loading -> showSnackbar(getString(R.string.feature_download_requested, getString(featureState.feature.labelResId)))
                is FeatureViewModel.FeatureState.Available -> {
                    Feature.values().find { featureState.modules.contains(it.moduleName) }?.let {
                        showSnackbar(getString(R.string.feature_downloaded, getString(it.labelResId)))
                        //after the dynamic feature module has been installed, we need to check if data needed by the module (e.g. Tesseract) has been downloaded
                        if (!featureViewModel.isFeatureAvailable(this, it)) {
                            featureViewModel.requestFeature(this, it)
                        } else {
                            onFeatureAvailable(it)
                        }
                    }
                }
                is FeatureViewModel.FeatureState.Error -> showSnackbar(featureState.throwable.toString())
            }
        })
        super.onCreate(savedInstanceState)
        tracker.init(this)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            CrashHandler.report(e)
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        tracker.setEnabled(enabled)
    }

    fun logEvent(event: String?, params: Bundle?) {
        tracker.logEvent(event, params)
    }

    @CallSuper
    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        try {
            resources.getResourceName(command)
        } catch (e: Resources.NotFoundException) {
            null
        }?.let { fullResourceName ->
            logEvent(Tracker.EVENT_DISPATCH_COMMAND, Bundle().apply {
                putString(Tracker.EVENT_PARAM_ITEM_ID, fullResourceName.substring(fullResourceName.indexOf('/') + 1))
            })
        }
        if (command == R.id.TESSERACT_DOWNLOAD_COMMAND) {
            ocrViewModel.downloadTessData().observe(this, {
                downloadPending = it
            })
            return true
        }
        return false
    }

    fun processImageCaptureError(resultCode: Int, activityResult: CropImage.ActivityResult?) {
        if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            showSnackbar(activityResult?.error?.let {
                if (it is ActivityNotFoundException) getString(R.string.image_capture_not_installed) else it.message
            } ?: "ERROR")
        }
    }

    fun showDismissableSnackbar(message: Int) {
        showDismissableSnackbar(getText(message))
    }

    fun showDismissableSnackbar(message: CharSequence) {
        showSnackbar(message, Snackbar.LENGTH_INDEFINITE,
                SnackbarAction(R.string.snackbar_dismiss) { snackbar?.dismiss() })
    }

    fun showSnackbar(message: Int) {
        showSnackbar(message, Snackbar.LENGTH_LONG)
    }

    fun showSnackbar(message: Int, duration: Int) {
        showSnackbar(getText(message), duration)
    }

    fun showSnackbar(message: CharSequence) {
        showSnackbar(message, Snackbar.LENGTH_LONG, null)
    }

    fun showSnackbar(message: CharSequence, duration: Int) {
        showSnackbar(message, duration, null)
    }

    open fun showSnackbar(message: CharSequence, duration: Int, snackbarAction: SnackbarAction?) {
        showSnackbar(message, duration, snackbarAction, null)
    }

    fun showSnackbar(message: CharSequence, duration: Int, snackbarAction: SnackbarAction?,
                     callback: Snackbar.Callback?) {
        val container = findViewById<View>(getSnackbarContainerId())
        if (container == null) {
            CrashHandler.report(String.format("Class %s is unable to display snackbar", javaClass))
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            showSnackbar(message, duration, snackbarAction, callback, container)
        }
    }

    fun showSnackbar(message: CharSequence, duration: Int, snackbarAction: SnackbarAction?,
                     callback: Snackbar.Callback?, container: View) {
        snackbar = Snackbar.make(container, message, duration).apply {
            UiUtils.increaseSnackbarMaxLines(this)
            if (snackbarAction != null) {
                setAction(snackbarAction.resId, snackbarAction.listener)
            }
            if (callback != null) {
                addCallback(callback)
            }
            show()
        }

    }

    fun dismissSnackbar() {
        snackbar?.dismiss()
    }

    @IdRes
    protected open fun getSnackbarContainerId(): Int {
        return R.id.fragment_container
    }

    fun offerTessDataDownload() {
        ocrViewModel.offerTessDataDownload(this)
    }

    fun checkTessDataDownload() {
        ocrViewModel.tessDataExists().observe(this, {
            if (!it)
                offerTessDataDownload()
        })
    }

    fun startActionView(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setData(Uri.parse(uri))
            })
        } catch (e: ActivityNotFoundException) {
            showSnackbar("No activity found for opening $uri", Snackbar.LENGTH_LONG, null)
        }
    }

    open fun showMessage(message: CharSequence) {
        showMessage(message, MessageDialogFragment.Button.okButton(), null, null)
    }

    open fun showMessage(message: CharSequence,
                         positive: MessageDialogFragment.Button?,
                         neutral: MessageDialogFragment.Button?,
                         negative: MessageDialogFragment.Button?,
                         cancellable: Boolean = true) {
        lifecycleScope.launchWhenResumed {
            MessageDialogFragment.newInstance(null, message, positive, neutral, negative).apply {
                setCancelable(cancellable)
            }.show(getSupportFragmentManager(), "MESSAGE")
        }
    }

    fun showVersionDialog(prev_version: Int, showImportantUpgradeInfo: Boolean) {
        lifecycleScope.launchWhenResumed {
            VersionDialogFragment.newInstance(prev_version, showImportantUpgradeInfo)
                    .show(getSupportFragmentManager(), "VERSION_INFO")
        }
    }

    fun unencryptedBackupWarning() = getString(R.string.warning_unencrypted_backup,
            getString(R.string.pref_security_export_passphrase_title))

    public override fun onMessageDialogDismissOrCancel() {}
}