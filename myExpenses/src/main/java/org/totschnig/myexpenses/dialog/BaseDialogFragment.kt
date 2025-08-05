package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.SnackbarAction
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.linkInputsWithLabels
import org.totschnig.myexpenses.util.ui.UiUtils
import timber.log.Timber
import javax.inject.Inject


abstract class BaseDialogFragment : DialogFragment() {
    protected lateinit var dialogView: View
    protected lateinit var materialLayoutInflater: LayoutInflater

    private var snackBar: Snackbar? = null

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var dataStore: DataStore<Preferences>


    open val fullScreenIfNotLarge = false

    protected open fun configureArguments(requestKey: String) = Bundle(1).apply {
        putString(KEY_REQUEST_KEY, requestKey)
    }

    val requestKey: String
        get() = requireArguments().getString(KEY_REQUEST_KEY)!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    private val isLarge
        get() = resources.getBoolean(R.bool.isLarge)

    protected val fullScreen
        get() = fullScreenIfNotLarge && !isLarge

    override fun onStart() {
        super.onStart()
        if (fullScreen) {
            dialog?.window?.let {
                val width = ViewGroup.LayoutParams.MATCH_PARENT
                val height = ViewGroup.LayoutParams.MATCH_PARENT
                it.setLayout(width, height)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBarsAndCutouts = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.setPadding(
                systemBarsAndCutouts.left,
                systemBarsAndCutouts.top,
                systemBarsAndCutouts.right,
                systemBarsAndCutouts.bottom
            )

            insets
        }
    }

    @SuppressLint("UseGetLayoutInflater")
    open fun initBuilder(): AlertDialog.Builder = (if (fullScreen)
        AlertDialog.Builder(requireContext(), R.style.FullscreenDialog)
    else MaterialAlertDialogBuilder(requireContext())
            ).also {
            materialLayoutInflater = LayoutInflater.from(it.context)
        }

    protected fun initBuilderWithLayoutResource(layoutResourceId: Int) =
        initBuilderWithView {
            it.inflate(layoutResourceId, null)
        }

    protected fun initBuilderWithView(inflate: (LayoutInflater) -> View) =
        initBuilder().also { builder ->
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

    protected fun showSnackBar(resId: Int, duration: Int = Snackbar.LENGTH_LONG) {
        showSnackBar(getString(resId), duration)
    }

    fun showSnackBar(
        message: CharSequence,
        duration: Int = Snackbar.LENGTH_LONG,
        snackBarAction: SnackbarAction? = null,
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
            TransactionDetailFragment.show(
                transactionId,
                parentFragmentManager
            )
        }
    }

    protected val snackBarContainer
        get() = if (::dialogView.isInitialized) dialogView else {
            CrashHandler.report(Exception("lateinit property dialogView has not been initialized"))
            dialog!!.window!!.decorView
        }

    protected fun dismissSnackBar() {
        snackBar?.dismiss()
    }
}