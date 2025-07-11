/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.totschnig.myexpenses.R

/**
 * This class presents a simple dialog asking user to confirm a message. Optionally the dialog can also
 * present a checkbox that allows user to provide some secondary decision. If the Bundle provided
 * in [.newInstance] provides an entry with key [KEY_PREFKEY], the value of the
 * checkbox will be stored in a preference with this key, and R.string.do_not_show_again
 * will be set as text for the checkbox. If the Bundle provides [KEY_CHECKBOX_LABEL], this will
 * be used as text for the checkbox label. In that case, the state of the checkbox will be communicated
 * in the second argument of [org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener.onPositive]
 */
class ConfirmationDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {
    private var checkBox: CheckBox? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bundle = requireArguments()
        val ctx: Activity = requireActivity()
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(ctx)
        val icon = bundle.getInt(KEY_ICON)
        if (icon != 0) {
            builder.setIcon(icon)
        }
        val title = bundle.getInt(KEY_TITLE, 0)
        if (title != 0) {
            builder.setTitle(title)
        } else {
            val titleString = bundle.getString(KEY_TITLE_STRING, null)
            if (titleString != null) {
                builder.setTitle(titleString)
            }
        }
        builder.setMessage(bundle.getCharSequence(KEY_MESSAGE))
        val checkboxLabel = bundle.getString(KEY_CHECKBOX_LABEL)
        val checkedLabel = bundle.getInt(KEY_POSITIVE_BUTTON_CHECKED_LABEL)
        val positiveLabel = bundle.getInt(KEY_POSITIVE_BUTTON_LABEL)
        val negativeLabel = bundle.getInt(KEY_NEGATIVE_BUTTON_LABEL)
        val initiallyChecked = bundle.getBoolean(KEY_CHECKBOX_INITIALLY_CHECKED, false)

        if (bundle.getString(KEY_PREFKEY) != null || checkboxLabel != null) {
            val cb = LayoutInflater.from(builder.context).inflate(R.layout.checkbox, null)

            checkBox = cb.findViewById<CheckBox>(R.id.checkBox).apply {
                text = checkboxLabel ?: getString(R.string.do_not_show_again)
                isChecked = initiallyChecked
                if (checkedLabel != 0) {
                    setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                        (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                            .setText(
                                if (isChecked) checkedLabel else positiveLabel
                            )
                    }
                }
            }
            builder.setView(cb)
        }

        builder.setPositiveButton(
            when {
                positiveLabel == 0 -> android.R.string.ok
                initiallyChecked -> checkedLabel
                else -> positiveLabel
            },
            this
        )
        builder.setNegativeButton(
            if (negativeLabel == 0) android.R.string.cancel else negativeLabel,
            this
        )
        val hasNeutral = bundle.getInt(KEY_COMMAND_NEUTRAL) != 0
        if (hasNeutral) {
            builder.setNeutralButton(bundle.getInt(KEY_NEUTRAL_BUTTON_LABEL), null)
        }
        return builder.create().apply {
            if (hasNeutral) {
                setOnShowListener { dialog ->
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener {
                            onClick(dialog, AlertDialog.BUTTON_NEUTRAL)
                        }
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        val ctx = activity as ConfirmationDialogListener?
        ctx?.onDismissOrCancel()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx = activity as ConfirmationDialogListener? ?: return
        val arguments = requireArguments()
        val prefKey = arguments.getString(KEY_PREFKEY)
        if (prefKey != null && checkBox!!.isChecked) {
            prefHandler.putBoolean(prefKey, true)
        }
        when (which) {
            AlertDialog.BUTTON_POSITIVE ->
                ctx.onPositive(arguments, checkBox?.isChecked == true)
            AlertDialog.BUTTON_NEUTRAL -> ctx.onNeutral(arguments)
            else -> {
                val negativeCommand = arguments.getInt(KEY_COMMAND_NEGATIVE)
                if (negativeCommand != 0) {
                    ctx.onNegative(arguments)
                } else {
                    onCancel(dialog)
                }
            }
        }
    }

    interface ConfirmationDialogListener {
        fun onNegative(args: Bundle)
        fun onNeutral(args: Bundle)
        fun onDismissOrCancel()
        fun onPositive(args: Bundle, checked: Boolean)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_TITLE_STRING = "titleString"
        const val KEY_MESSAGE = "message"
        const val KEY_COMMAND_POSITIVE = "positiveCommand"
        const val KEY_COMMAND_NEGATIVE = "negativeCommand"
        const val KEY_COMMAND_NEUTRAL = "neutralCommand"
        const val KEY_TAG_POSITIVE_BUNDLE = "positiveTagBundle"
        const val KEY_PREFKEY = "prefKey"
        const val KEY_CHECKBOX_LABEL = "checkboxLabel"
        const val KEY_CHECKBOX_INITIALLY_CHECKED = "checkboxInitiallyChecked"
        const val KEY_POSITIVE_BUTTON_LABEL = "positiveButtonLabel"
        const val KEY_POSITIVE_BUTTON_CHECKED_LABEL = "positiveButtonCheckedLabel"
        const val KEY_NEGATIVE_BUTTON_LABEL = "negativeButtonLabel"
        const val KEY_NEUTRAL_BUTTON_LABEL = "neutralButtonLabel"
        const val KEY_ICON = "icon"

        @JvmStatic
        fun newInstance(args: Bundle): ConfirmationDialogFragment {
            val dialogFragment = ConfirmationDialogFragment()
            dialogFragment.arguments = args
            return dialogFragment
        }
    }
}