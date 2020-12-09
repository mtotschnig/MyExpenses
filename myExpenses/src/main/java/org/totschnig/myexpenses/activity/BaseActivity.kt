package org.totschnig.myexpenses.activity

import android.content.ActivityNotFoundException
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

internal abstract class BaseActivity : AppCompatActivity() {
    private var snackbar: Snackbar? = null
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

}