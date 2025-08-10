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
import android.os.Bundle
import android.text.*
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.databinding.ExportDialogBinding
import org.totschnig.myexpenses.export.AbstractExporter.Companion.ENCODING_LATIN_1
import org.totschnig.myexpenses.export.AbstractExporter.Companion.ENCODING_UTF_8
import org.totschnig.myexpenses.export.AbstractExporter.Companion.ENCODING_UTF_8_BOM
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ui.configurePopupAnchor
import org.totschnig.myexpenses.util.ui.postScrollToBottom
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_CREATE_HELPER
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_DO_NOTHING
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
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
import java.util.Date
import java.util.Locale

class ExportDialogFragment : DialogViewBinding<ExportDialogBinding>(),
    DialogInterface.OnClickListener {

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

        if (accountInfo.id == HOME_AGGREGATE_ID) {
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
            dialogView.findViewById<View>(R.id.with_filter).isVisible = true
            warningText = getString(R.string.warning_reset_account_matched)
        }

        binding.format.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (checkedId == R.id.csv) {
                binding.DelimiterRow.isVisible = isChecked
                if (!isChecked && binding.Encoding.checkedButtonId == R.id.utf8_bom) {
                    binding.Encoding.check(R.id.utf8)
                }
                binding.utf8Bom.isVisible = isChecked
                configureDateTimeFormat()
            }
        }
        val format = prefHandler.enumValueOrDefault(PrefKey.EXPORT_FORMAT, ExportFormat.QIF)
        binding.format.check(format.resId)

        class DateFormatWatcher(val editText: EditText) : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                try {
                    DateTimeFormatter.ofPattern(s.toString())
                    editText.error = null
                } catch (_: IllegalArgumentException) {
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
                } catch (_: IllegalArgumentException) {
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
                } catch (_: IllegalArgumentException) {
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

        val encoding = prefHandler.getString(PREF_KEY_EXPORT_ENCODING, ENCODING_UTF_8)
        binding.Encoding.check(
            when (encoding) {
                ENCODING_UTF_8_BOM -> R.id.utf8_bom
                ENCODING_LATIN_1 -> R.id.iso88591
                else -> R.id.utf8
            }
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

        if (savedInstanceState == null) {
            when (prefHandler.getInt(
                KEY_EXPORT_HANDLE_DELETED, EXPORT_HANDLE_DELETED_CREATE_HELPER
            )) {
                EXPORT_HANDLE_DELETED_UPDATE_BALANCE -> {
                    binding.handleDeleted.check(R.id.update_balance)
                }
                EXPORT_HANDLE_DELETED_CREATE_HELPER -> {
                    binding.handleDeleted.check(R.id.create_helper)
                }
            }
        }
        if (accountInfo.cannotResetConditions.isEmpty()) {
            binding.exportDelete.setOnCheckedChangeListener { _, isChecked ->
                configure(isChecked)
                if (isChecked) {
                    binding.root.postScrollToBottom()
                }
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setText(
                    if (isChecked) R.string.menu_reset else R.string.menu_export
                )
            }
        } else {
            binding.exportDelete.isEnabled = false
            binding.exportDeleteDisabledText.isVisible = true
            binding.exportDeleteDisabledText.text = accountInfo.cannotResetConditions.joinToString {
                getString(it)
            }
        }
        if (accountInfo.hasExported) {
            binding.exportNotYetExported.isChecked = true
            binding.exportNotYetExported.isVisible = true
        }
        binding.warningReset.text = warningText
        if (allP) {
            val mergeAccounts = prefHandler.getBoolean(KEY_MERGE_P, false)
            setFileNameLabel(false)
            binding.mergeAccounts.isVisible = true
            binding.mergeAccounts.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                setFileNameLabel(
                    isChecked
                )
            }
            binding.mergeAccounts.isChecked = mergeAccounts
        }

        dialogView.findViewById<View>(R.id.date_format_help).configurePopupAnchor(
            infoText = buildDateFormatHelpText()
        )

        builder.setTitle(if (allP) R.string.menu_reset_all else R.string.menu_reset)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.menu_export, this)
        return builder.create()
    }

    private val splitDateTime: Boolean
        get() = prefHandler.getBoolean(PrefKey.CSV_EXPORT_SPLIT_DATE_TIME, false) &&
                binding.format.checkedButtonId == R.id.csv

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
        val format = ExportFormat.entries.find { it.resId == binding.format.checkedButtonId }
            ?: ExportFormat.QIF
        val dateFormat = binding.dateFormat.text.toString()
        val timeFormat = binding.timeFormat.text.toString()
        val decimalSeparator = if (binding.separator.checkedButtonId == R.id.dot) '.' else ','
        val delimiter = when (binding.Delimiter.checkedButtonId) {
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
        val handleDeleted = when (binding.handleDeleted.checkedButtonId) {
            R.id.update_balance -> {
                EXPORT_HANDLE_DELETED_UPDATE_BALANCE
            }
            R.id.create_helper -> {
                EXPORT_HANDLE_DELETED_CREATE_HELPER
            }
            else -> {
                EXPORT_HANDLE_DELETED_DO_NOTHING
            }
        }
        val encoding = when (binding.Encoding.checkedButtonId) {
            R.id.utf8_bom -> ENCODING_UTF_8_BOM
            R.id.iso88591 -> ENCODING_LATIN_1
            else -> ENCODING_UTF_8
        }
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
                if (accountInfo.id != HOME_AGGREGATE_ID) {
                    putString(DatabaseConstants.KEY_CURRENCY, accountInfo.currency)
                }
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
        binding.warningReset.isVisible = delete
        binding.handleDeleted.isVisible = delete
    }

    private fun configureButton() {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            binding.dateFormat.error == null && binding.timeFormat.error == null &&
                    binding.fileName.error == null
    }

    override fun onStart() {
        super.onStart()
        configure(binding.exportDelete.isChecked)
    }

    data class AccountInfo(
        val id: Long,
        val label: String,
        val currency: String,
        val cannotResetConditions: List<Int>,
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