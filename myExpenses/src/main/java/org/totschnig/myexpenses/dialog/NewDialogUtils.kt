package org.totschnig.myexpenses.dialog

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import eltos.simpledialogfragment.color.SimpleColorDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.adapter.GroupedSpinnerAdapter
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.enumValueOrDefault

fun Spinner.configureDateFormat(
    context: Context,
    prefHandler: PrefHandler,
    prefName: String,
) {
    adapter = ArrayAdapter(
        context, android.R.layout.simple_spinner_item, QifDateFormat.entries.toTypedArray()
    ).apply {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }
    setSelection(prefHandler.enumValueOrDefault(prefName, QifDateFormat.default).ordinal)
}

fun buildColorDialog(context: Context, color: Int?): SimpleColorDialog = SimpleColorDialog.build()
    .allowCustom(true)
    .cancelable(false)
    .colorNames(context, R.array.material_color_names)
    .neut()
    .apply {
        color?.let { colorPreset(it) }
    }

fun ContentResolver.getDisplayName(
    uri: Uri,
): String {

    if (!"file".equals(uri.scheme, ignoreCase = true)) {
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        try {
            query(uri, null, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    // Note it's called "Display Name".  This is
                    // provider-specific, and might not necessarily be the file name.
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        val displayName = it.getString(columnIndex)
                        if (displayName != null) {
                            return displayName
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            //this can happen if the user has restored a backup and
            //we do not have a persistable permission
            null
        }
    }
    return uri.lastPathSegment ?: "UNKNOWN"
}

fun Spinner.configureCurrencySpinner(listener: AdapterView.OnItemSelectedListener? = null): CurrencyAdapter {
    val curAdapter = CurrencyAdapter(context, android.R.layout.simple_spinner_item)
    setAdapter(curAdapter)
    onItemSelectedListener = listener
    return curAdapter
}

fun Spinner.configureTypeSpinner() = GroupedSpinnerAdapter<Boolean, AccountType>(
    context,
    itemToString = { it.localizedName(context) },
    headerToString = { context.getString(if(it) R.string.balance_sheet_section_assets else R.string.balance_sheet_section_liabilities) }
).also {
    setAdapter(it)
}

fun GroupedSpinnerAdapter<Boolean, AccountType>.addAll(data: List<AccountType>) {
    clear()
    addAll(data.groupBy { it.isAsset }.let {
        listOfNotNull(
            it[true]?.let { assets -> true to assets.sortedBy { it.localizedName(context) } },
            it[false]?.let { liabilities -> false to liabilities.sortedBy { it.localizedName(context) } }
        )
    })
}