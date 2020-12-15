package org.totschnig.myexpenses.activity

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
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

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        injectDependencies()
    }

    protected open fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker.init(this)
        ocrViewModel = ViewModelProvider(this).get(OcrViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(downloadReceiver)
    }

    fun setTrackingEnabled(enabled: Boolean) {
        tracker.setEnabled(enabled)
    }

    fun logEvent(event: String?, params: Bundle?) {
        tracker.logEvent(event, params)
    }

    @CallSuper
    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        val bundle = Bundle()
        val fullResourceName = resources.getResourceName(command)
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, fullResourceName.substring(fullResourceName.indexOf('/') + 1))
        logEvent(Tracker.EVENT_DISPATCH_COMMAND, bundle)
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
}