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

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.databinding.ExportDialogBinding
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.postScrollToBottom
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_DATE_FORMAT
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_DECIMAL_SEPARATOR
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_DELETE_P
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_DELIMITER
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_ENCODING
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_EXPORT_HANDLE_DELETED
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_FILE_NAME
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_FORMAT
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_MERGE_P
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_NOT_YET_EXPORTED_P
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.KEY_TIME_FORMAT
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

class ExportDialogFragment : DialogViewBinding<ExportDialogBinding>(),
    DialogInterface.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    private var handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val accountInfo = requireArguments().getSerializable(KEY_DATA) as AccountInfo
        var allP = false
        var warningText: String
        val fileName: String
        val now = SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
            .format(Date())
        val builder = initBuilder {
            ExportDialogBinding.inflate(it)
        }

        val canReset = !accountInfo.isSealed
        if (accountInfo.id == Account.HOME_AGGREGATE_ID) {
            allP = true
            warningText = getString(R.string.warning_reset_account_all, "")
            fileName = "export-$now"
        } else {
            if (accountInfo.id < 0L) {
                allP = true
                fileName = "export-${accountInfo.currency}-$now"
                warningText =
                    getString(R.string.warning_reset_account_all, " (${accountInfo.currency})")
            } else {
                fileName = Utils.escapeForFileName(accountInfo.label) + "-" + now
                warningText = getString(R.string.warning_reset_account)
            }
        }
        if (accountInfo.isFiltered) {
            dialogView!!.findViewById<View>(R.id.with_filter).visibility = View.VISIBLE
            warningText = getString(R.string.warning_reset_account_matched)
        }

        binding.format.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            binding.DelimiterRow.visibility =
                if (checkedId == R.id.csv) View.VISIBLE else View.GONE
            configureDateTimeFormat()
        }
        val format = enumValueOrDefault(
            prefHandler.getString(PrefKey.EXPORT_FORMAT, null),
            ExportFormat.QIF
        )
        binding.format.check(format.resId)

        class DateFormatWatcher(val editText: EditText) : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                try {
                    DateTimeFormatter.ofPattern(s.toString())
                    editText.error = null
                } catch (e: IllegalArgumentException) {
                    editText.error = getString(R.string.date_format_illegal)
                }
                configureButton()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }

        val dateFormat = prefHandler.getString(PREF_KEY_EXPORT_DATE_FORMAT, "")
            ?.takeIf {
                it.isNotEmpty() && (try {
                    DateTimeFormatter.ofPattern(it)
                } catch (e: IllegalArgumentException) {
                    null
                }) != null
            } ?: DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT,
            null,
            IsoChronology.INSTANCE, Locale.getDefault()
        )
        binding.dateFormat.setText(dateFormat)
        binding.dateFormat.addTextChangedListener(DateFormatWatcher(binding.dateFormat))

        val timeFormat = prefHandler.getString(PREF_KEY_EXPORT_TIME_FORMAT, "")
            ?.takeIf {
                it.isNotEmpty() && (try {
                    DateTimeFormatter.ofPattern(it)
                } catch (e: IllegalArgumentException) {
                    null
                }) != null
            } ?: DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            null,
            FormatStyle.SHORT,
            IsoChronology.INSTANCE, Locale.getDefault()
        )
        binding.timeFormat.setText(timeFormat)
        binding.timeFormat.addTextChangedListener(DateFormatWatcher(binding.timeFormat))
        configureDateTimeFormat()

        binding.fileName.setText(fileName)
        binding.fileName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                var error = 0
                if (s.toString().isNotEmpty()) {
                    if (s.toString().indexOf('/') > -1) {
                        error = R.string.slash_forbidden_in_filename
                    }
                } else {
                    error = R.string.required
                }
                binding.fileName.error = if (error != 0) getString(error) else null
                configureButton()
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
        binding.fileName.filters = arrayOf(
            InputFilter { source: CharSequence, start: Int, end: Int, _: Spanned?, _: Int, _: Int ->
                val sb = StringBuilder(end - start)
                for (i in start until end) {
                    val c = source[i]
                    val type = Character.getType(c)
                    if (type != Character.SURROGATE.toInt() && type != Character.OTHER_SYMBOL.toInt()) {
                        sb.append(c)
                    }
                }
                sb
            }
        )

        val encoding = prefHandler.getString(PREF_KEY_EXPORT_ENCODING, "UTF-8")
        binding.Encoding.setSelection(
            listOf(*resources.getStringArray(R.array.pref_qif_export_file_encoding))
                .indexOf(encoding)
        )

        val delimiter = prefHandler.getInt(KEY_DELIMITER, ','.code)
            .toChar()
        @IdRes val delimiterButtonResId = when (delimiter) {
            ';' -> R.id.delimiter_semicolon
            '\t' -> R.id.delimiter_tab
            ',' -> R.id.delimiter_comma
            else -> R.id.delimiter_comma
        }
        binding.Delimiter.check(delimiterButtonResId)
        val separator = prefHandler.getInt(
            KEY_DECIMAL_SEPARATOR, Utils.getDefaultDecimalSeparator().code
        )
            .toChar()
        binding.separator.check(if (separator == ',') R.id.comma else R.id.dot)
        val radioClickListener = View.OnClickListener { v: View ->
            val mappedAction =
                if (v.id == R.id.create_helper) Account.EXPORT_HANDLE_DELETED_CREATE_HELPER else Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
            if (handleDeletedAction == mappedAction) {
                handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING
                binding.handleDeleted.clearCheck()
            } else {
                handleDeletedAction = mappedAction
            }
        }
        val updateBalanceRadioButton =
            dialogView!!.findViewById<RadioButton>(R.id.update_balance)
        val createHelperRadioButton = dialogView!!.findViewById<RadioButton>(R.id.create_helper)
        updateBalanceRadioButton.setOnClickListener(radioClickListener)
        createHelperRadioButton.setOnClickListener(radioClickListener)
        if (savedInstanceState == null) {
            handleDeletedAction = prefHandler.getInt(
                KEY_EXPORT_HANDLE_DELETED, Account.EXPORT_HANDLE_DELETED_CREATE_HELPER
            )
            if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
                updateBalanceRadioButton.isChecked = true
            } else if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_CREATE_HELPER) {
                createHelperRadioButton.isChecked = true
            }
        }
        if (canReset) {
            binding.exportDelete.setOnCheckedChangeListener(this)
        } else {
            binding.exportDelete.visibility = View.GONE
        }
        if (accountInfo.hasExported) {
            binding.exportNotYetExported.isChecked = true
            binding.exportNotYetExported.visibility = View.VISIBLE
        }
        binding.warningReset.text = warningText
        if (allP) {
            val mergeAccounts = prefHandler.getBoolean(KEY_MERGE_P, false)
            setFileNameLabel(false)
            binding.mergeAccounts.visibility = View.VISIBLE
            binding.mergeAccounts.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                setFileNameLabel(
                    isChecked
                )
            }
            binding.mergeAccounts.isChecked = mergeAccounts
        }
        val helpIcon = dialogView!!.findViewById<View>(R.id.date_format_help)
        helpIcon.setOnClickListener {
            val inflater = LayoutInflater.from(activity)
            val infoTextView = inflater.inflate(
                R.layout.textview_info, null
            ) as TextView
            val infoText = buildDateFormatHelpText()
            val infoWindow = PopupWindow(infoTextView)
            infoWindow.setBackgroundDrawable(BitmapDrawable())
            infoWindow.isOutsideTouchable = true
            infoWindow.isFocusable = true
            chooseSize(infoWindow, infoText, infoTextView)
            infoTextView.text = infoText
            infoTextView.movementMethod = LinkMovementMethod.getInstance()
            //Linkify.addLinks(infoTextView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            infoWindow.showAsDropDown(helpIcon)
        }
        builder.setTitle(if (allP) R.string.menu_reset_all else R.string.menu_reset)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, this)
        return builder.create()
    }

    private val splitDateTime: Boolean
        get() = prefHandler.getBoolean(PrefKey.CSV_EXPORT_SPLIT_DATE_TIME, false) &&
                binding.format.checkedRadioButtonId == R.id.csv

    private fun configureDateTimeFormat() {
        with(splitDateTime) {
            binding.timeFormat.isVisible = this
            binding.DateFormatLabel.text = getString(R.string.date_format) +
                    if (this) " / " + getString(R.string.time_format) else ""
        }
    }

    private fun setFileNameLabel(oneFile: Boolean) {
        binding.fileNameLabel.setText(if (oneFile) R.string.file_name else R.string.folder_name)
    }

    /* adapted from android.widget.Editor */
    private fun chooseSize(pop: PopupWindow, text: CharSequence, tv: TextView) {
        var ht = tv.paddingTop + tv.paddingBottom
        val widthInPixels = (dialog!!.window!!.decorView.width * 0.75).toInt()
        val l: Layout = StaticLayout(
            text, tv.paint, widthInPixels,
            Layout.Alignment.ALIGN_NORMAL, 1F, 0F, true
        )
        ht += l.height
        pop.width = widthInPixels
        pop.height = ht
    }

    private fun buildDateFormatHelpText(): CharSequence {
        val letters = resources.getStringArray(R.array.help_ExportDialog_date_format_letters)
        val components = resources.getStringArray(R.array.help_ExportDialog_date_format_components)
        val sb = StringBuilder()
        for (i in letters.indices) {
            sb.append(letters[i])
            sb.append(" => ")
            sb.append(components[i])
            if (i < letters.size - 1) sb.append(", ") else sb.append(". ")
        }
        return TextUtils.concat(
            sb,
            HtmlCompat.fromHtml(
                getString(R.string.help_ExportDialog_date_format, "https://developer.android.com/reference/java/time/format/DateTimeFormatter"),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (activity == null) {
            return
        }
        val accountInfo = requireArguments().getSerializable(KEY_DATA) as AccountInfo
        val format = ExportFormat.values().find { it.resId == binding.format.checkedRadioButtonId }
            ?: ExportFormat.QIF
        val dateFormat = binding.dateFormat.text.toString()
        val timeFormat = binding.timeFormat.text.toString()
        val decimalSeparator = if (binding.separator.checkedRadioButtonId == R.id.dot) '.' else ','
        val delimiter = when (binding.Delimiter.checkedRadioButtonId) {
            R.id.delimiter_tab -> {
                '\t'
            }
            R.id.delimiter_semicolon -> {
                ';'
            }
            else -> {
                ','
            }
        }
        val handleDeleted = when (binding.handleDeleted.checkedRadioButtonId) {
            R.id.update_balance -> {
                Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
            }
            R.id.create_helper -> {
                Account.EXPORT_HANDLE_DELETED_CREATE_HELPER
            }
            else -> {
                Account.EXPORT_HANDLE_DELETED_DO_NOTHING
            }
        }
        val encoding = binding.Encoding.selectedItem as String
        with(prefHandler) {
            putString(PrefKey.EXPORT_FORMAT, format.name)
            putString(PREF_KEY_EXPORT_DATE_FORMAT, dateFormat)
            if (splitDateTime) {
                putString(PREF_KEY_EXPORT_TIME_FORMAT, timeFormat)
            }
            putString(PREF_KEY_EXPORT_ENCODING, encoding)
            putInt(KEY_DECIMAL_SEPARATOR, decimalSeparator.code)
            putInt(KEY_DELIMITER, delimiter.code)
            putInt(KEY_EXPORT_HANDLE_DELETED, handleDeleted)
        }
        (requireActivity() as MyExpenses).startExport(Bundle().apply {
            putInt(
                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                R.id.START_EXPORT_COMMAND
            )
            if (accountInfo.id > 0) {
                putLong(DatabaseConstants.KEY_ROWID, accountInfo.id)
            } else {
                putString(DatabaseConstants.KEY_CURRENCY, accountInfo.currency)
                val mergeAccounts = binding.mergeAccounts.isChecked
                putBoolean(KEY_MERGE_P, mergeAccounts)
                prefHandler.putBoolean(KEY_MERGE_P, mergeAccounts)
            }
            putSerializable(KEY_FORMAT, format)
            putBoolean(KEY_DELETE_P, binding.exportDelete.isChecked)
            putBoolean(KEY_NOT_YET_EXPORTED_P, binding.exportNotYetExported.isChecked)
            putString(KEY_DATE_FORMAT, dateFormat)
            if (splitDateTime) {
                putString(KEY_TIME_FORMAT, timeFormat)
            }
            putChar(KEY_DECIMAL_SEPARATOR, decimalSeparator)
            putString(KEY_ENCODING, encoding)
            putInt(KEY_EXPORT_HANDLE_DELETED, handleDeleted)
            putString(KEY_FILE_NAME, binding.fileName.text.toString())
            putChar(KEY_DELIMITER, delimiter)
        })
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        configure(isChecked)
        if (isChecked) {
            binding.root.postScrollToBottom()
        }
    }

    /*
   * if we are in the situation, where there are already exported transactions
   * we suggest to the user the default of again exporting without deleting
   * but if the user now changes to deleting, we enforce a complete export/reset
   * since a partial deletion of only transactions not yet exported would
   * lead to an inconsistent state
   */
    private fun configure(delete: Boolean) {
        binding.exportNotYetExported.isEnabled = !delete
        binding.exportNotYetExported.isChecked = !delete
        binding.warningReset.visibility =
            if (delete) View.VISIBLE else View.GONE
        binding.handleDeleted.visibility =
            if (delete) View.VISIBLE else View.GONE
    }

    private fun configureButton() {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            binding.dateFormat.error == null && binding.timeFormat.error == null &&
                    binding.fileName.error == null
    }

    override fun onStart() {
        super.onStart()
        configure(binding.exportDelete.isChecked)
        val checkedId = binding.handleDeleted.checkedRadioButtonId
        if (checkedId == R.id.update_balance) handleDeletedAction =
            Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE else if (checkedId == R.id.create_helper) handleDeletedAction =
            Account.EXPORT_HANDLE_DELETED_CREATE_HELPER
    }

    data class AccountInfo(
        val id: Long,
        val label: String,
        val currency: String,
        val isSealed: Boolean,
        val hasExported: Boolean,
        val isFiltered: Boolean
    ) : Serializable

    companion object {
        private const val KEY_DATA = "data"
        const val PREF_KEY_EXPORT_DATE_FORMAT = "export_date_format"
        const val PREF_KEY_EXPORT_TIME_FORMAT = "export_time_format"
        const val PREF_KEY_EXPORT_ENCODING = "export_encoding"

        fun newInstance(accountInfo: AccountInfo) = ExportDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_DATA, accountInfo)
            }
        }
    }
}