package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog

class NewMessageDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): MaterialDialog {
        val arguments = requireArguments()
        return MaterialDialog(requireContext())
                .message(text = arguments.getCharSequence(KEY_MESSAGE),
                        applySettings = { if (arguments.getBoolean(KEY_HTML)) html() }
                )
                .positiveButton(android.R.string.ok)
    }

    companion object {
        const val KEY_MESSAGE = "message"
        const val KEY_HTML = "html"
        fun newInstance(message: CharSequence, html: Boolean = false) = NewMessageDialogFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(KEY_MESSAGE, message)
                putBoolean(KEY_HTML, html)
            }
        }
    }
}