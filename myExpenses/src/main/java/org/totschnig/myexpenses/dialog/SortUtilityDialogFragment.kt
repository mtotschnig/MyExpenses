package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.RecyclerListAdapter
import org.totschnig.myexpenses.adapter.SortableItem
import org.totschnig.myexpenses.adapter.helper.OnStartDragListener
import org.totschnig.myexpenses.adapter.helper.SimpleItemTouchHelperCallback
import java.util.*

class SortUtilityDialogFragment : BaseDialogFragment(), OnStartDragListener,
    DialogInterface.OnClickListener {
    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var callback: OnConfirmListener
    private lateinit var adapter: RecyclerListAdapter

    interface OnConfirmListener {
        fun onSortOrderConfirmed(sortedIds: LongArray)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = try {
            context as OnConfirmListener
        } catch (_: ClassCastException) {
            throw ClassCastException(
                context.toString()
                        + " must implement OnConfirmListener"
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder()
        val args = savedInstanceState ?: requireArguments()
        adapter = RecyclerListAdapter(
            this,
            BundleCompat.getParcelableArrayList(args, KEY_ITEMS, SortableItem::class.java)!!
        )
        val recyclerView = RecyclerView(builder.context)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(adapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper.attachToRecyclerView(recyclerView)
        return builder.setTitle(R.string.sort_order)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .setView(recyclerView)
            .create()
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        callback.onSortOrderConfirmed(adapter.items.map { it.id }.toLongArray())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_ITEMS, adapter.items)
    }

    companion object {
        private const val KEY_ITEMS = "items"
        @JvmStatic
        fun newInstance(items: ArrayList<SortableItem>): SortUtilityDialogFragment {
            val fragment = SortUtilityDialogFragment()
            val args = Bundle(1)
            args.putParcelableArrayList(KEY_ITEMS, items)
            fragment.arguments = args
            return fragment
        }
    }
}