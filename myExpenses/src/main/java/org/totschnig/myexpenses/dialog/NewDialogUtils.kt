package org.totschnig.myexpenses.dialog

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import eltos.simpledialogfragment.color.SimpleColorDialog
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.adapter.GroupedSpinnerAdapter
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.viewmodel.data.Account

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

fun GroupedSpinnerAdapter<Boolean, AccountType>.addAllAccountTypes(data: List<AccountType>) {
    clear()
    addAll(data.groupBy { it.isAsset }.let { map ->
        listOfNotNull(
            map[true]?.let { assets -> true to assets.sortedBy { it.localizedName(context) } },
            map[false]?.let { liabilities -> false to liabilities.sortedBy { it.localizedName(context) } }
        )
    })
}

fun GroupedSpinnerAdapter<AccountFlag, Account>.addAllAccounts(data: List<Account>) {
    clear()
    addAll(data.groupBy { it.flag }.toList().sortedByDescending { it.first.sortKey })
}

fun buildPartyEditDialog(partyId: Long?, name:String?, shortName:String?) = SimpleFormDialog.build()
    .fields(
        Input.name(KEY_PAYEE_NAME)
            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            .required()
            .hint(R.string.full_name)
            .text(name),
        Input.name(KEY_SHORT_NAME)
            .hint(R.string.nickname)
            .text(shortName)
            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    )
    .title(if (partyId == null) R.string.menu_create_party else R.string.menu_edit_party)
    .cancelable(false)
    .pos(if (partyId == null) R.string.menu_add else R.string.menu_save)
    .neut()
    .extra(Bundle().apply {
        putLong(KEY_ROWID, partyId ?: 0L)
    })