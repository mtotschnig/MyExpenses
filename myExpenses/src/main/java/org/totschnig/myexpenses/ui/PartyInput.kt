package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.os.Build
import android.os.Parcelable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.FilterQueryProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.SimpleCursorAdapter
import android.widget.ViewSwitcher
import androidx.annotation.RequiresApi
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.chip.Chip
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.databinding.PartyInputBinding
import org.totschnig.myexpenses.dialog.buildPartyEditDialog
import org.totschnig.myexpenses.fragment.PartiesList.Companion.DIALOG_EDIT_PARTY
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

@Parcelize
data class DisplayParty(
    val id: Long?,
    val name: String,
    val shortName: String? = null
): Parcelable {

    val displayName: String
        get() = shortName ?: name

    companion object {
        fun fromCursor(cursor: Cursor) = cursor.getLongOrNull(KEY_PAYEEID)?.let {
            DisplayParty(
                it,
                cursor.getString(KEY_PAYEE_NAME),
                cursor.getStringOrNull(KEY_SHORT_NAME)
            )
        }
    }
}

class PartyInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    // Declare a variable for the binding class
    // Use the new layout file name here
    private val binding =
        PartyInputBinding.inflate(LayoutInflater.from(context), this, true)

    private val viewSwitcher: ViewSwitcher
        get() = binding.viewSwitcher
    private val autoCompleteTextView: AutoCompleteTextView
        get() = binding.autoCompleteTextview
    private val buttonClear: ImageView
        get() = binding.buttonClear
    private val selectedItemChip: Chip
        get() = binding.selectedItemChip

    val partyForSave: DisplayParty?
        get() = party ?: autoCompleteTextView.text.toString().takeIf { it.isNotEmpty() }?.let {
            DisplayParty(null, it)
        }

    @State
    var party: DisplayParty? = null
        set(value) {
            field = value
            if (value?.id == null) {
                autoCompleteTextView.setText(value?.name)
                switchView(INDEX_AUTOCOMPLETE_TEXTVIEW)
            } else {
                selectedItemChip.text = value.name
                switchView(INDEX_CHIP_CONTAINER)
            }
        }

    private lateinit var payeeAdapter: SimpleCursorAdapter
    private lateinit var partySelectListener: PartySelectListener

    override fun onSaveInstanceState() =
        StateSaver.saveInstanceState(this, super.onSaveInstanceState())

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(StateSaver.restoreInstanceState(this, state))
    }

    fun interface PartySelectListener {
        fun onPartySelected(partyId: Long?)
    }

    var text: String?
        get() = autoCompleteTextView.text.toString()
        set(value) {
            autoCompleteTextView.setText(value)
            // If text is set programmatically, we might need to reset the 'disabled' state
            // or decide on the desired behavior. For now, let's assume setting text re-enables.
            switchView(INDEX_AUTOCOMPLETE_TEXTVIEW)
        }

    var hint: CharSequence?
        get() = autoCompleteTextView.hint
        set(value) {
            autoCompleteTextView.hint = value
        }

    init {

        buttonClear.setOnClickListener {
            party = null
            switchView(INDEX_AUTOCOMPLETE_TEXTVIEW)
            autoCompleteTextView.requestFocus()
        }

        selectedItemChip.setOnCloseIconClickListener {
            buildPartyEditDialog(party?.id, party?.name, party?.shortName)
                .show(host, DIALOG_EDIT_PARTY)
        }
        selectedItemChip.closeIconContentDescription = context.getString(R.string.menu_edit_party)
    }

    private fun switchView(index: Int) {
        viewSwitcher.displayedChild = index
    }

    fun addTextChangedListener(watcher: TextWatcher) {
        autoCompleteTextView.addTextChangedListener(watcher)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setInputMethodModeNeeded() {
        autoCompleteTextView.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
    }


    fun createPayeeAdapter(
        withInputMethodNeeded: Boolean = false,
        partySelectListener: PartySelectListener
    ) {
        this.partySelectListener = partySelectListener
        payeeAdapter = SimpleCursorAdapter(
            context,
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            null,
            arrayOf(KEY_PAYEE_NAME),
            intArrayOf(android.R.id.text1),
            0
        )
        if (withInputMethodNeeded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setInputMethodModeNeeded()
            }
        }
        autoCompleteTextView.setAdapter(payeeAdapter)
        payeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
            if (constraint != null) {
                val (selection, selectArgs) =
                    " AND ${Party.SELECTION}" to Party.selectionArgs(
                        Utils.escapeSqlLikeExpression(Utils.normalize(constraint.toString()))
                    )
                context.contentResolver.query(
                    TransactionProvider.PAYEES_URI,
                    arrayOf(KEY_ROWID, KEY_PAYEE_NAME, KEY_SHORT_NAME),
                    "$KEY_PARENTID IS NULL $selection",
                    selectArgs,
                    null
                )
            } else null
        }
        payeeAdapter.stringConversionColumn = 1
        autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val c = payeeAdapter.getItem(position) as Cursor
                if (c.moveToPosition(position)) {
                    val partyId = c.getLong(0)
                    val name = c.getString(1)
                    val shortName = c.getString(2)
                    party = DisplayParty(partyId, name, shortName)
                    partySelectListener.onPartySelected(partyId)
                }
            }
    }

    fun onDestroy() {
        if (::payeeAdapter.isInitialized) {
            payeeAdapter.cursor?.let {
                if (!it.isClosed) {
                    it.close()
                }
            }
        } else {
            CrashHandler.report(IllegalStateException("PayeeAdapter not initialized"))
        }
    }

    private val host: ExpenseEdit
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is ExpenseEdit) {
                    return context
                }
                context = context.baseContext
            }
            throw IllegalStateException("Host context expected to be ExpenseEdit")
        }

    companion object {
        // Constants for ViewSwitcher child indices
        private const val INDEX_AUTOCOMPLETE_TEXTVIEW = 0
        private const val INDEX_CHIP_CONTAINER = 1
    }
}

