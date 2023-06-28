package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BackupRestoreActivity
import org.totschnig.myexpenses.dialog.DialogUtils.CalendarRestoreStrategyChangedListener
import org.totschnig.myexpenses.util.ImportFileResultHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report

class BackupSourcesDialogFragment() : ImportSourceDialogFragment(),
    CalendarRestoreStrategyChangedListener {
    private lateinit var restorePlanStrategy: RadioGroup
    private lateinit var mCalendarRestoreButtonCheckedChangeListener: RadioGroup.OnCheckedChangeListener
    private lateinit var encrypt: CheckBox
    override val withSelectFromAppFolder = true

    //Normally, it is recommended to pass configuration to fragment via setArguments,
    //but since we safe uri in instance state, it is safe to set it in constructor
    constructor(data: Uri?) : this() {
        mUri = data
    }

    override val layoutId = R.layout.backup_restore_dialog

    override fun setupDialogView(view: View) {
        super.setupDialogView(view)
        val selectorVisibility =
            if ((requireActivity() as BackupRestoreActivity).calledExternally) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.summary).visibility = selectorVisibility
        view.findViewById<View>(R.id.btn_browse).visibility = selectorVisibility
        restorePlanStrategy = configureCalendarRestoreStrategy(view, prefHandler)
        mCalendarRestoreButtonCheckedChangeListener =
            DialogUtils.buildCalendarRestoreStrategyChangedListener(
                activity, this
            )
        restorePlanStrategy.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener)
        encrypt = view.findViewById(R.id.encrypt_database)
        if (prefHandler.encryptDatabase) {
            encrypt.isVisible = true
            encrypt.isChecked = true
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (savedInstanceState == null && mUri != null) {
            try {
                ImportFileResultHandler.handleFilenameRequestResult(this, mUri)
            } catch (throwable: Throwable) {
                mUri = null
                Toast.makeText(requireContext(), throwable.message, Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
        }
        return dialog
    }

    override val layoutTitle: String
        get() = getString(R.string.pref_restore_title)

    override fun getTypeName() = "Zip"

    override fun getPrefKey() = "backup_restore_file_uri"

    override fun checkTypeParts(mimeType: String, extension: String): Boolean {
        val typeParts = mimeType.split("/")
        if (typeParts[0] == "application" && ((typeParts[1] == "zip") || (typeParts[1]) == "octet-stream")
            && (extension == "" || extension == "zip" || extension == "enc")
        ) return true
        if (extension == "zip" || extension == "enc") {
            report(
                Exception(
                    "Found resource with extension $extension and unexpected mime type ${typeParts[0]}/${typeParts[1]}"
                )
            )
            return true
        }
        return false
    }

    override fun onClick(dialog: DialogInterface, id: Int) {
        if (activity == null) {
            return
        }
        if (id == AlertDialog.BUTTON_POSITIVE) {
            (activity as BackupRestoreActivity).onSourceSelected(
                mUri!!,
                restorePlanStrategy.checkedRadioButtonId,
                prefHandler.encryptDatabase && encrypt.isChecked
            )
        } else {
            super.onClick(dialog, id)
        }
    }

    override val isReady: Boolean
        get() = if (super.isReady) {
            restorePlanStrategy.checkedRadioButtonId != -1
        } else {
            false
        }

    override fun onCheckedChanged() {
        setButtonState()
    }

    override fun onCalendarPermissionDenied() {
        with(restorePlanStrategy) {
            setOnCheckedChangeListener(null)
            clearCheck()
            setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener)
        }
        setButtonState()
    }

    companion object {
        fun newInstance(data: Uri?): BackupSourcesDialogFragment {
            return BackupSourcesDialogFragment(data)
        }
    }
}