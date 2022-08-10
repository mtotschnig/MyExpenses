package org.totschnig.myexpenses.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
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
    protected lateinit var materialLayoutInflater: LayoutInflater

    private var snackBar: Snackbar? = null

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    open fun initBuilder(): AlertDialog.Builder =
        MaterialAlertDialogBuilder(requireContext()).also {
            materialLayoutInflater = LayoutInflater.from(it.context)
        }

    protected fun initBuilderWithLayoutResource(layoutResourceId: Int) =
        initBuilderWithView {
            it.inflate(layoutResourceId, null)
        }

    protected fun initBuilderWithView(inflate: (LayoutInflater) -> View) = initBuilder().also {
        dialogView = inflate(materialLayoutInflater)
        it.setView(dialogView)
    }

    fun report(e: IllegalStateException) {
        activity?.also {
            Timber.w("Activity is finishing?: %b", it.isFinishing)
        } ?: run {
            Timber.w("Activity is null")
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

    protected fun showSnackBar(resId: Int) {
        showSnackBar(getString(resId))
    }

    fun showSnackBar(
        message: CharSequence,
        duration: Int = Snackbar.LENGTH_LONG,
        snackBarAction: SnackbarAction? = null
    ) {
        val view = dialogView ?: dialog!!.window!!.decorView
        snackBar = Snackbar.make(view, message, duration).also {
            UiUtils.increaseSnackbarMaxLines(it)
            if (snackBarAction != null) {
                it.setAction(snackBarAction.resId, snackBarAction.listener)
            }
            it.show()
        }
    }

    protected fun dismissSnackBar() {
        snackBar?.dismiss()
    }
}