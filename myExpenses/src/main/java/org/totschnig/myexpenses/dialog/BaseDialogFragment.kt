package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TableLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.linkInputsWithLabels
import timber.log.Timber
import javax.inject.Inject

abstract class BaseDialogFragment : DialogFragment() {
    protected lateinit var dialogView: View
    protected lateinit var materialLayoutInflater: LayoutInflater

    private var snackBar: Snackbar? = null

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    @SuppressLint("UseGetLayoutInflater")
    open fun initBuilder(): AlertDialog.Builder =
        MaterialAlertDialogBuilder(requireContext()).also {
            materialLayoutInflater = LayoutInflater.from(it.context)
        }

    protected fun initBuilderWithLayoutResource(layoutResourceId: Int) =
        initBuilderWithView {
            it.inflate(layoutResourceId, null)
        }

    protected fun initBuilderWithView(inflate: (LayoutInflater) -> View) = initBuilder().also { builder ->
        dialogView = inflate(materialLayoutInflater).also { view ->
            view.findViewById<TableLayout>(R.id.FormTable)?.let {
                linkInputsWithLabels(it)
            }
        }
        builder.setView(dialogView)
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
        snackBar = Snackbar.make(snackBarContainer, message, duration).also {
            UiUtils.increaseSnackbarMaxLines(it)
            if (snackBarAction != null) {
                it.setAction(snackBarAction.label, snackBarAction.listener)
            }
            it.show()
        }
    }

    fun showDetails(transactionId: Long) {
        lifecycleScope.launchWhenResumed {
            TransactionDetailFragment.show(transactionId, parentFragmentManager)
        }
    }

    protected val snackBarContainer
        get() = dialogView

    protected fun dismissSnackBar() {
        snackBar?.dismiss()
    }
}