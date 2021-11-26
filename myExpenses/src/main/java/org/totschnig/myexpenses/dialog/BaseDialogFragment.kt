//on some occasions, upon showing a DialogFragment we run into
//"java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
//we catch this here, and ignore silently, which hopefully should be save, since activity is being paused
//https://code.google.com/p/android/issues/detail?id=23096#c4
package org.totschnig.myexpenses.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import javax.inject.Inject

abstract class BaseDialogFragment : DialogFragment() {
    @JvmField
    protected var dialogView: View? = null
    lateinit var materialLayoutInflater: LayoutInflater

    private var snackbar: Snackbar? = null

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    protected fun initBuilder(): AlertDialog.Builder =
            MaterialAlertDialogBuilder(requireContext()).also {
                materialLayoutInflater = LayoutInflater.from(it.context)
            }

    protected fun initBuilderWithBinding(inflate: () -> ViewBinding): AlertDialog.Builder =
            initBuilder().also {
                dialogView = inflate().root
                it.setView(dialogView)
            }

    protected fun initBuilderWithView(layoutResourceId: Int): AlertDialog.Builder =
            initBuilder().also {
                dialogView = materialLayoutInflater.inflate(layoutResourceId, null)
                it.setView(dialogView)
            }

    fun report(e: IllegalStateException?) {
        val activity = activity
        if (activity == null) {
            Timber.w("Activity is null")
        } else {
            Timber.w("Activity is finishing?: %b", activity.isFinishing)
        }
        CrashHandler.report(e)
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: IllegalStateException) {
            report(e)
        }
    }

    protected fun showSnackbar(resId: Int) {
        showSnackbar(getString(resId))
    }

    fun showSnackbar(message: CharSequence, duration: Int = Snackbar.LENGTH_LONG, snackbarAction: SnackbarAction? = null) {
        val view = dialogView ?: dialog!!.window!!.decorView
        snackbar = Snackbar.make(view, message, duration).also {
            UiUtils.increaseSnackbarMaxLines(it)
            if (snackbarAction != null) {
                it.setAction(snackbarAction.resId, snackbarAction.listener)
            }
            it.show()
        }
    }

    protected fun dismissSnackbar() {
        snackbar?.dismiss()
    }
}