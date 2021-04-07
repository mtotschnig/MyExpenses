package org.totschnig.myexpenses.fragment

import android.util.SparseBooleanArray
import android.view.ActionMode
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ExpandableListView
import android.widget.ExpandableListView.*
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity

/**
 * @author Michael Totschnig
 * provide helper functionality to create a CAB for a ListView
 */
@Deprecated("")
abstract class ContextualActionBarFragment : Fragment(), OnGroupClickListener, OnChildClickListener {
    @JvmField
    var mActionMode: ActionMode? = null

    @JvmField
    var expandableListSelectionType = PACKED_POSITION_TYPE_NULL

    private val menuSingleIds = intArrayOf(R.id.EDIT_COMMAND,
            R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND,
            R.id.SELECT_COMMAND, R.id.VIEW_COMMAND, R.id.CREATE_INSTANCE_EDIT_COMMAND,
            R.id.CREATE_TEMPLATE_COMMAND, R.id.CLONE_TRANSACTION_COMMAND)
    private val menuSingleGroupIds = intArrayOf(R.id.CREATE_SUB_COMMAND, R.id.COLOR_COMMAND)

    open fun dispatchCommandSingle(command: Int, info: ContextMenuInfo?): Boolean {
        val ctx = requireActivity() as ProtectedFragmentActivity
        val handled = ctx.dispatchCommand(command, info)
        if (handled) {
            finishActionMode()
        }
        return handled
    }

    /**
     * dispatch a bulk command with the provided information about checked positions and itemIds
     * subclasses that override this method should not assume that the count and order of positions
     * is in parallel with the itemIds, it should do its work either based on positions or based on itemIds
     */
    open fun dispatchCommandMultiple(command: Int, positions: SparseBooleanArray?, itemIds: Array<Long?>?): Boolean {
        val ctx = requireActivity() as ProtectedFragmentActivity
        //we send only the positions to the default dispatch command mechanism,
        //but subclasses can provide a method that handles the itemIds
        val handled = ctx.dispatchCommand(command, positions)
        if (handled) {
            finishActionMode()
        }
        return handled
    }

    protected open val menuResource = 0

    protected open fun withCommonContext(): Boolean {
        return true
    }

    protected open fun inflateContextualActionBar(menu: Menu, listId: Int) {
        val inflater = requireActivity().menuInflater
        if (withCommonContext()) {
            inflater.inflate(R.menu.common_context, menu)
        }
        if (menuResource != 0) inflater.inflate(menuResource, menu)
    }

    protected open fun shouldStartActionMode() = true

    open fun setTitle(mode: ActionMode, lv: AbsListView) {
        val count = lv.checkedItemCount
        mode.title = count.toString()
    }

    open fun onSelectionChanged(position: Int, checked: Boolean) {}

    open fun onFinishActionMode() {}

    fun registerForContextualActionBar(lv: AbsListView) {
        lv.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        lv.setMultiChoiceModeListener(object : MultiChoiceModeListener {
            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int,
                                                   id: Long, checked: Boolean) {
                val count = lv.checkedItemCount
                if (lv is ExpandableListView && count == 1 && checked) {
                    expandableListSelectionType = getPackedPositionType(
                            lv.getExpandableListPosition(position))
                }
                onSelectionChanged(position, checked)
                setTitle(mode, lv)
                configureMenu(mode.menu, lv)
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                //After orientation change,
                //setting expandableListSelectionType, as tried in setExpandableListSelectionType
                //does not work, because getExpandableListPosition does not return the correct value
                //probably because the adapter has not yet been set up correctly
                //thus we default to PACKED_POSITION_TYPE_GROUP
                //this workaround works because orientation change collapses the groups
                //so we never restore the CAB for PACKED_POSITION_TYPE_CHILD
                if (!shouldStartActionMode()) return false
                expandableListSelectionType = if (lv is ExpandableListView) PACKED_POSITION_TYPE_GROUP else PACKED_POSITION_TYPE_NULL
                inflateContextualActionBar(menu, lv.id)
                setTitle(mode, lv)
                mActionMode = mode
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                configureMenu(menu, lv)
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val itemId = item.itemId
                //this allows us to have main menu entries without id that just open the submenu
                if (itemId == 0) return false
                if (itemId == R.id.SELECT_ALL_COMMAND) {
                    val adapter = lv.adapter.let { (it as? HeaderViewListAdapter)?.wrappedAdapter ?: it }
                    for (i in 0 until adapter.count) {
                        if (!lv.isItemChecked(i)) {
                            lv.setItemChecked(i, (lv as? ExpandableListView)?.getExpandableListPosition(i)?.let { expandableListSelectionType == getPackedPositionType(it) } ?: true)
                        }
                    }
                    return true
                }
                val checkedItemPositions = lv.checkedItemPositions
                val checkedItemCount = checkedItemPositions.size()
                var result = false
                if (isSingleCommand(item) || isSingleGroupCommand(item)) {
                    for (i in 0 until checkedItemCount) {
                        if (checkedItemPositions.valueAt(i)) {
                            val position = checkedItemPositions.keyAt(i)
                            val info: ContextMenuInfo
                            val id: Long
                            if (lv is ExpandableListView) {
                                val pos = lv.getExpandableListPosition(position)
                                val groupPos = getPackedPositionGroup(pos)
                                id = if (getPackedPositionType(pos) == PACKED_POSITION_TYPE_GROUP) {
                                    lv.expandableListAdapter.getGroupId(groupPos)
                                } else {
                                    val childPos = getPackedPositionChild(pos)
                                    lv.expandableListAdapter.getChildId(groupPos, childPos)
                                }
                                //getChildAt returned null in some cases
                                //thus we decide to not rely on it
                                info = ExpandableListContextMenuInfo(null, pos, id)
                            } else {
                                val v = lv.getChildAt(position)
                                id = lv.getItemIdAtPosition(position)
                                info = AdapterContextMenuInfo(v, position, id)
                            }
                            result = dispatchCommandSingle(itemId, info)
                            break
                        }
                    }
                } else {
                    val itemIdsObj: Array<Long?>
                    if (lv is ExpandableListView) {
                        itemIdsObj = arrayOfNulls(checkedItemCount)
                        for (i in 0 until checkedItemCount) {
                            if (checkedItemPositions.valueAt(i)) {
                                val position = checkedItemPositions.keyAt(i)
                                val pos = lv.getExpandableListPosition(position)
                                val groupPos = getPackedPositionGroup(pos)
                                if (getPackedPositionType(pos) == PACKED_POSITION_TYPE_GROUP) {
                                    itemIdsObj[i] = lv.expandableListAdapter.getGroupId(groupPos)
                                } else {
                                    val childPos = getPackedPositionChild(pos)
                                    itemIdsObj[i] = lv.expandableListAdapter.getChildId(groupPos, childPos)
                                }
                            }
                        }
                    } else {
                        val itemIdsPrim = lv.checkedItemIds
                        itemIdsObj = arrayOfNulls(itemIdsPrim.size)
                        for (i in itemIdsPrim.indices) {
                            itemIdsObj[i] = itemIdsPrim[i]
                        }
                    }
                    //TODO:should we convert the flat positions here?
                    result = dispatchCommandMultiple(
                            itemId,
                            checkedItemPositions,
                            itemIdsObj)
                }
                //mode.finish();
                return result
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                onFinishActionMode()
                mActionMode = null
            }
        })
        if (lv is ExpandableListView) {
            lv.setOnGroupClickListener(this)
            lv.setOnChildClickListener(this)
        }
    }

    override fun onGroupClick(parent: ExpandableListView, v: View, groupPosition: Int, id: Long): Boolean {
        if (mActionMode != null) {
            if (expandableListSelectionType == PACKED_POSITION_TYPE_GROUP) {
                val flatPosition = parent.getFlatListPosition(getPackedPositionForGroup(groupPosition))
                parent.setItemChecked(
                        flatPosition,
                        !parent.isItemChecked(flatPosition))
                return true
            }
        }
        return false
    }

    override fun onChildClick(parent: ExpandableListView, v: View,
                              groupPosition: Int, childPosition: Int, id: Long): Boolean {
        if (mActionMode != null) {
            if (expandableListSelectionType == PACKED_POSITION_TYPE_CHILD) {
                val flatPosition = parent.getFlatListPosition(
                        getPackedPositionForChild(groupPosition, childPosition))
                parent.setItemChecked(
                        flatPosition,
                        !parent.isItemChecked(flatPosition))
            }
            return true
        }
        return false
    }

    protected open fun configureMenu(menu: Menu, lv: AbsListView) {
        val inGroup = expandableListSelectionType == PACKED_POSITION_TYPE_GROUP
        for (i in 0 until menu.size()) {
            with(menu.getItem(i)) {
                if (isSingleCommand(this)) {
                    isVisible = lv.checkedItemCount == 1
                }
                if (isSingleGroupCommand(this)) {
                    isVisible = inGroup && lv.checkedItemCount == 1
                }
            }
        }
    }

    private fun isSingleCommand(item: MenuItem) = menuSingleIds.contains(item.itemId)
    private fun isSingleGroupCommand(item: MenuItem) = menuSingleGroupIds.contains(item.itemId)

    fun finishActionMode() {
        mActionMode?.finish()
        onFinishActionMode()
    }

    fun invalidateCAB() {
        mActionMode?.invalidate()
    }
}