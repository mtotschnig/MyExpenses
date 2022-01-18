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
package org.totschnig.myexpenses.dialog.select

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.dialog.select.DataHolder.Companion.fromCursor
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import javax.inject.Inject

abstract class SelectFromTableDialogFragment(private val withNullItem: Boolean) :
    BaseDialogFragment(), DialogInterface.OnClickListener {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver
    private lateinit var itemDisposable: Disposable
    protected lateinit var adapter: ArrayAdapter<DataHolder?>
    protected open val dialogTitle: Int = 0
    abstract val uri: Uri
    abstract val column: String
    protected open val selectionArgs: Array<String?>? = null
    protected open val selection: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!itemDisposable.isDisposed) {
            itemDisposable.dispose()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (dialog as AlertDialog).listView.checkedItemPositions?.let {
            outState.putParcelable(KEY_CHECKED_POSITIONS, SparseBooleanArrayParcelable(it))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val projection = arrayOf(DatabaseConstants.KEY_ROWID, column)
        val layout =
            if (choiceMode == AbsListView.CHOICE_MODE_MULTIPLE) android.R.layout.simple_list_item_multiple_choice else android.R.layout.simple_list_item_single_choice
        adapter = object : ArrayAdapter<DataHolder?>(requireContext(), layout) {
            override fun hasStableIds(): Boolean {
                return true
            }

            override fun getItem(position: Int): DataHolder? {
                //workaround for framework bug, which causes getItem to be called upon orientation change with invalid position
                return if (count == 0) null else super.getItem(position)
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position)
                if (item!!.id != EMPTY_ITEM_ID) return super.getView(position, convertView, parent)
                val inflater = LayoutInflater.from(parent.context)
                @SuppressLint("ViewHolder") val textView =
                    inflater.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
                textView.text = item.label
                return textView
            }

            override fun getItemId(position: Int): Long {
                val item = getItem(position)
                return item?.id ?: AdapterView.INVALID_ROW_ID
            }
        }
        itemDisposable = briteContentResolver.createQuery(
            uri,
            projection, selection, selectionArgs, null, false
        )
            .mapToList { cursor: Cursor ->
                fromCursor(cursor, column)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { collection: List<DataHolder?> ->
                adapter.clear()
                val activity: Activity? = activity
                if (activity != null) {
                    val alertDialog = dialog as AlertDialog
                    if (withNullItem) {
                        adapter.add(DataHolder(NULL_ITEM_ID, getString(R.string.unmapped)))
                    } else if (collection.isEmpty()) {
                        val neutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        if (neutral != null) {
                            neutral.visibility = View.GONE
                        }
                        val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        if (positive != null) {
                            positive.visibility = View.GONE
                        }
                        adapter.add(DataHolder(EMPTY_ITEM_ID, emptyMessage))
                        alertDialog.listView.choiceMode = AbsListView.CHOICE_MODE_NONE
                    }
                    adapter.addAll(collection)
                    adapter.notifyDataSetChanged()
                    if (savedInstanceState != null) {
                        val checkedItemPositions: SparseBooleanArrayParcelable? =
                            savedInstanceState.getParcelable(
                                KEY_CHECKED_POSITIONS
                            )
                        if (checkedItemPositions != null) {
                            for (i in 0 until checkedItemPositions.size()) {
                                if (checkedItemPositions.valueAt(i)) {
                                    alertDialog.listView.setItemChecked(
                                        checkedItemPositions.keyAt(i),
                                        true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        val neutralButton = neutralButton
        val negativeButton = negativeButton
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(
            requireActivity()
        )
            .setAdapter(adapter, null)
            .setPositiveButton(positiveButton, null)
        val dialogTitle = dialogTitle
        if (dialogTitle != 0) {
            builder.setTitle(dialogTitle)
        }
        if (neutralButton != 0) {
            builder.setNeutralButton(neutralButton, null)
        }
        if (negativeButton != 0) {
            builder.setNegativeButton(negativeButton, null)
        }
        val alertDialog = builder.create()
        alertDialog.listView.itemsCanFocus = false
        alertDialog.listView.choiceMode = choiceMode
        //prevent automatic dismiss on button click
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onClick(
                    alertDialog,
                    AlertDialog.BUTTON_POSITIVE
                )
            }
            val neutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutral?.setOnClickListener {
                onClick(
                    alertDialog,
                    AlertDialog.BUTTON_NEUTRAL
                )
            }
        }
        return alertDialog
    }

    private val emptyMessage: String
        get() {
            val arguments = arguments
            val resId: Int
            if (arguments != null) {
                resId = arguments.getInt(KEY_EMPTY_MESSAGE)
                if (resId != 0) return getString(resId)
            }
            return "No data"
        }
    protected open val choiceMode: Int
        get() = AbsListView.CHOICE_MODE_MULTIPLE
    protected open val neutralButton: Int
        get() = 0
    protected open val negativeButton: Int
        get() = android.R.string.cancel
    protected open val positiveButton: Int
        get() = android.R.string.ok

    abstract override fun onClick(dialog: DialogInterface, which: Int)

    companion object {
        private const val KEY_CHECKED_POSITIONS = "checked_positions"
        const val KEY_DIALOG_TITLE = "dialog_tile"
        const val KEY_EMPTY_MESSAGE = "empty_message"
        const val NULL_ITEM_ID = 0L
        protected const val EMPTY_ITEM_ID = -1L
    }
}