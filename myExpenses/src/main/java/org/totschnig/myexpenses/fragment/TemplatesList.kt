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
package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.SparseBooleanArray
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.*
import org.totschnig.myexpenses.databinding.TemplatesListBinding
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForTemplatesWithPlans
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import java.io.Serializable
import java.lang.ref.WeakReference
import javax.inject.Inject

const val KEY_INSTANCES = "instances"
const val DIALOG_TAG_CONFIRM_CANCEL = "confirm_cancel"
const val DIALOG_TAG_CONFIRM_RESET = "confirm_reset"

class TemplatesList : SortableListFragment(), LoaderManager.LoaderCallbacks<Cursor?>,
    SimpleDialog.OnDialogResultListener {
    private var popup: PopupMenu? = null
    override val menuResource: Int
        get() = R.menu.templateslist_context
    private var mTemplatesCursor: Cursor? = null
    private var mAdapter: SimpleCursorAdapter? = null
    lateinit var mManager: LoaderManager
    private var columnIndexAmount = 0
    private var columnIndexComment = 0
    private var columnIndexPayee = 0
    private var columnIndexColor = 0
    private var columnIndexDefaultAction = 0
    private var columnIndexCurrency = 0
    private var columnIndexTransferAccount = 0
    private var columnIndexPlanId = 0
    private var columnIndexTitle = 0
    private var columnIndexRowId = 0
    private var columnIndexPlanInfo = 0
    private var columnIndexIsSealed = 0
    private var indexesCalculated = false
    private var hasPlans = false

    /**
     * if we are called from the calendar app, we only need to handle display of plan once
     */
    @JvmField
    @State
    var expandedHandled = false

    @JvmField
    @State
    var repairTriggered = false

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    lateinit var viewModel: TemplatesListViewModel

    private var _binding: TemplatesListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Icepick.restoreInstanceState(this, savedInstanceState)
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        viewModel = ViewModelProvider(this)[TemplatesListViewModel::class.java]
        appComponent.inject(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireActivity() as ProtectedFragmentActivity
        _binding = TemplatesListBinding.inflate(inflater, container, false)
        mManager = LoaderManager.getInstance(this)
        mManager.initLoader(SORTABLE_CURSOR, null, this)
        // Create an array to specify the fields we want to display in the list
        val from = arrayOf(
            DatabaseConstants.KEY_TITLE,
            DatabaseConstants.KEY_LABEL,
            DatabaseConstants.KEY_AMOUNT
        )
        // and an array of the fields we want to bind those fields to
        val to = intArrayOf(R.id.title, R.id.category, R.id.amount)
        mAdapter = MyAdapter(
            ctx,
            R.layout.template_row,
            null,
            from,
            to,
            0
        )
        binding.list.adapter = mAdapter
        binding.list.emptyView = binding.empty
        binding.list.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, id: Long ->
            if (mTemplatesCursor == null || !mTemplatesCursor!!.moveToPosition(position)) return@setOnItemClickListener
            val isSealed = mTemplatesCursor!!.getInt(columnIndexIsSealed) != 0
            if (isSealed) {
                ctx.showSnackBar(R.string.object_sealed)
            }
            if (mTemplatesCursor!!.isNull(columnIndexPlanId)) {
                if (!isSealed) {
                    if (isForeignExchangeTransfer(position)) {
                        dispatchCreateInstanceEditDo(id)
                    } else {
                        val splitAtPosition = isSplitAtPosition(position)
                        val defaultAction: Template.Action = enumValueOrDefault(
                            mTemplatesCursor!!.getString(columnIndexDefaultAction),
                            Template.Action.SAVE
                        )
                        if (defaultAction == Template.Action.SAVE) {
                            if (splitAtPosition) {
                                requestSplitTransaction(longArrayOf(id))
                            } else {
                                dispatchCreateInstanceSaveDo(longArrayOf(id))
                            }
                        } else {
                            if (splitAtPosition) {
                                requestSplitTransaction(id)
                            } else {
                                dispatchCreateInstanceEditDo(id)
                            }
                        }
                    }
                }
            } else {
                if (isCalendarPermissionGranted) {
                    val planMonthFragment = PlanMonthFragment.newInstance(
                        mTemplatesCursor!!.getString(columnIndexTitle),
                        id,
                        mTemplatesCursor!!.getLong(columnIndexPlanId),
                        mTemplatesCursor!!.getInt(columnIndexColor), isSealed
                    )
                    if (!childFragmentManager.isStateSaved) {
                        planMonthFragment.show(childFragmentManager, CALDROID_DIALOG_FRAGMENT_TAG)
                    }
                } else {
                    ctx.requestCalendarPermission()
                }
            }
        }
        registerForContextualActionBar(binding.list)
        return binding.root
    }

    private val isCalendarPermissionGranted: Boolean
        get() = PermissionGroup.CALENDAR.hasPermission(requireContext())

    private fun bulkUpdateDefaultAction(
        itemIds: LongArray,
        action: Template.Action,
        resultFeedBack: Int
    ) {
        viewModel.updateDefaultAction(itemIds, action).observe(
            viewLifecycleOwner
        ) { result: Boolean -> showSnackbar(if (result) getString(resultFeedBack) else "Error while setting default action for template click") }
    }

    override fun dispatchCommandMultiple(
        command: Int,
        positions: SparseBooleanArray, itemIds: LongArray
    ): Boolean {
        if (super.dispatchCommandMultiple(command, positions, itemIds)) {
            return true
        }
        when (command) {
            R.id.DEFAULT_ACTION_EDIT_COMMAND -> {
                bulkUpdateDefaultAction(
                    itemIds,
                    Template.Action.EDIT,
                    R.string.menu_create_instance_edit
                )
                return true
            }
            R.id.DEFAULT_ACTION_SAVE_COMMAND -> {
                bulkUpdateDefaultAction(
                    itemIds,
                    Template.Action.SAVE,
                    R.string.menu_create_instance_save
                )
                return true
            }
            R.id.DELETE_COMMAND -> {
                MessageDialogFragment.newInstance(
                    getString(R.string.dialog_title_warning_delete_template),  //TODO check if template
                    resources.getQuantityString(
                        R.plurals.warning_delete_template,
                        itemIds.size,
                        itemIds.size
                    ),
                    MessageDialogFragment.Button(
                        R.string.menu_delete,
                        R.id.DELETE_COMMAND_DO,
                        itemIds
                    ),
                    null,
                    MessageDialogFragment.Button(
                        R.string.response_no,
                        R.id.CANCEL_CALLBACK_COMMAND,
                        null
                    )
                )
                    .show(requireActivity().supportFragmentManager, "DELETE_TEMPLATE")
                return true
            }
            R.id.CREATE_INSTANCE_SAVE_COMMAND -> {
                if (hasSplitAtPositions(positions)) {
                    requestSplitTransaction(itemIds)
                } else {
                    dispatchCreateInstanceSaveDo(itemIds)
                }
                finishActionMode()
                return true
            }
            else -> return false
        }
    }

    override fun dispatchCommandSingle(command: Int, info: ContextMenuInfo?): Boolean {
        if (super.dispatchCommandSingle(command, info)) {
            return true
        }
        val menuInfo = info as AdapterContextMenuInfo
        val i: Intent
        if (command == R.id.CREATE_INSTANCE_EDIT_COMMAND) {
            if (isSplitAtPosition(menuInfo.position)) {
                requestSplitTransaction(menuInfo.id)
            } else {
                dispatchCreateInstanceEditDo(menuInfo.id)
            }
            finishActionMode()
            return true
        } else if (command == R.id.EDIT_COMMAND) {
            finishActionMode()
            i = Intent(activity, ExpenseEdit::class.java)
            i.putExtra(DatabaseConstants.KEY_TEMPLATEID, menuInfo.id)
            //TODO check what to do on Result
            startActivityForResult(i, EDIT_REQUEST)
            return true
        }
        return false
    }

    private fun isSplitAtPosition(position: Int): Boolean {
        return if (mTemplatesCursor != null) {
            mTemplatesCursor!!.moveToPosition(position) && DatabaseConstants.SPLIT_CATID == DbUtils.getLongOrNull(
                mTemplatesCursor,
                DatabaseConstants.KEY_CATID
            )
        } else false
    }

    private fun hasSplitAtPositions(positions: SparseBooleanArray): Boolean {
        for (i in 0 until positions.size()) {
            if (positions.valueAt(i) && isSplitAtPosition(positions.keyAt(i))) {
                return true
            }
        }
        return false
    }

    /**
     * calls [ProtectedFragmentActivity.contribFeatureRequested]
     * for feature [ContribFeature.SPLIT_TRANSACTION]
     *
     * @param tag if tag holds a single long the new instance will be edited, if tag holds an array of longs
     * new instances will be immediately saved for each
     */
    private fun requestSplitTransaction(tag: Serializable?) {
        (requireActivity() as ProtectedFragmentActivity).contribFeatureRequested(
            ContribFeature.SPLIT_TRANSACTION,
            tag
        )
    }

    fun dispatchCreateInstanceSaveDo(templateIds: LongArray) {
        dispatchCreateInstanceSaveDo(*templateIds.map { PlanInstanceInfo(it) }.toTypedArray())
    }

    private fun dispatchCreateInstanceSaveDo(vararg plans: PlanInstanceInfo) {
        viewModel.newFromTemplate(plans).observe(
            viewLifecycleOwner
        ) { successCount: Int ->
            showSnackbar(
                if (successCount == 0) getString(R.string.save_transaction_error) else resources.getQuantityString(
                    R.plurals.save_transaction_from_template_success,
                    successCount,
                    successCount
                )
            )
        }
    }

    fun dispatchCreateInstanceEditDo(itemId: Long) {
        val intent = Intent(requireActivity(), ExpenseEdit::class.java)
        intent.putExtra(DatabaseConstants.KEY_TEMPLATEID, itemId)
        intent.putExtra(DatabaseConstants.KEY_INSTANCEID, -1L)
        startActivity(intent)
    }

    private fun dispatchCreateInstanceEdit(templateId: Long, instanceId: Long, date: Long) {
        val intent = Intent(requireActivity(), ExpenseEdit::class.java)
        intent.putExtra(DatabaseConstants.KEY_TEMPLATEID, templateId)
        intent.putExtra(DatabaseConstants.KEY_INSTANCEID, instanceId)
        intent.putExtra(DatabaseConstants.KEY_DATE, date)
        startActivity(intent)
    }

    private fun dispatchEditInstance(transactionId: Long?) {
        val intent = Intent(requireActivity(), ExpenseEdit::class.java)
        intent.putExtra(DatabaseConstants.KEY_ROWID, transactionId)
        startActivityForResult(intent, EDIT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_REQUEST && resultCode == Activity.RESULT_OK) {
            val fragment = plannerFragment
            fragment?.onEditRequestOk()
        }
    }

    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor?> {
        return if (id == SORTABLE_CURSOR) {
            CursorLoader(
                requireActivity(),
                TransactionProvider.TEMPLATES_URI.buildUpon()
                    .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO, "1")
                    .build(),
                null,
                DatabaseConstants.KEY_PARENTID + " is null",
                null,
                preferredOrderByForTemplatesWithPlans(prefHandler, Sort.USAGES)
            )
        } else throw IllegalArgumentException()
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, c: Cursor?) {
        val ctx = requireActivity() as ManageTemplates
        if (loader.id == SORTABLE_CURSOR) {
            mTemplatesCursor = c
            if (c != null && !indexesCalculated) {
                columnIndexRowId = c.getColumnIndex(DatabaseConstants.KEY_ROWID)
                columnIndexAmount = c.getColumnIndex(DatabaseConstants.KEY_AMOUNT)
                columnIndexComment = c.getColumnIndex(DatabaseConstants.KEY_COMMENT)
                columnIndexPayee = c.getColumnIndex(DatabaseConstants.KEY_PAYEE_NAME)
                columnIndexColor = c.getColumnIndex(DatabaseConstants.KEY_COLOR)
                columnIndexCurrency = c.getColumnIndex(DatabaseConstants.KEY_CURRENCY)
                columnIndexTransferAccount =
                    c.getColumnIndex(DatabaseConstants.KEY_TRANSFER_ACCOUNT)
                columnIndexPlanId = c.getColumnIndex(DatabaseConstants.KEY_PLANID)
                columnIndexTitle = c.getColumnIndex(DatabaseConstants.KEY_TITLE)
                columnIndexPlanInfo = c.getColumnIndex(DatabaseConstants.KEY_PLAN_INFO)
                columnIndexIsSealed = c.getColumnIndex(DatabaseConstants.KEY_SEALED)
                columnIndexDefaultAction = c.getColumnIndex(DatabaseConstants.KEY_DEFAULT_ACTION)
                indexesCalculated = true
            }
            invalidateCAB()
            hasPlans = false
            if (isCalendarPermissionGranted && mTemplatesCursor != null && mTemplatesCursor!!.moveToFirst()) {
                val needToExpand =
                    if (expandedHandled) ManageTemplates.NOT_CALLED.toLong() else ctx.calledFromCalendarWithId
                var planMonthFragment: PlanMonthFragment? = null
                while (!mTemplatesCursor!!.isAfterLast) {
                    val planId = mTemplatesCursor!!.getLong(columnIndexPlanId)
                    if (planId != 0L) {
                        hasPlans = true
                    }
                    val templateId = mTemplatesCursor!!.getLong(columnIndexRowId)
                    if (needToExpand == templateId) {
                        planMonthFragment = PlanMonthFragment.newInstance(
                            mTemplatesCursor!!.getString(columnIndexTitle),
                            templateId,
                            planId,
                            mTemplatesCursor!!.getInt(columnIndexColor),
                            mTemplatesCursor!!.getInt(columnIndexIsSealed) != 0
                        )
                    }
                    mTemplatesCursor!!.moveToNext()
                }
                if (needToExpand != ManageTemplates.NOT_CALLED.toLong()) {
                    expandedHandled = true
                    if (planMonthFragment != null) {
                        planMonthFragment.show(childFragmentManager, CALDROID_DIALOG_FRAGMENT_TAG)
                    } else {
                        ctx.showSnackBar(R.string.save_transaction_template_deleted)
                    }
                }
                //look for plans that we could possible relink
                if (!repairTriggered && mTemplatesCursor!!.moveToFirst()) {
                    val missingUuids = ArrayList<String>()
                    while (!mTemplatesCursor!!.isAfterLast) {
                        if (!mTemplatesCursor!!.isNull(columnIndexPlanId) && mTemplatesCursor!!.isNull(
                                columnIndexPlanInfo
                            )
                        ) {
                            missingUuids.add(
                                mTemplatesCursor!!.getString(
                                    mTemplatesCursor!!.getColumnIndexOrThrow(
                                        DatabaseConstants.KEY_UUID
                                    )
                                )
                            )
                        }
                        mTemplatesCursor!!.moveToNext()
                    }
                    if (missingUuids.size > 0) {
                        RepairHandler(this).obtainMessage(
                            0, missingUuids.toTypedArray()
                        )
                            .sendToTarget()
                    }
                }
            }
            mAdapter!!.swapCursor(mTemplatesCursor)
            requireActivity().invalidateOptionsMenu()
        }
    }

    fun showSnackbar(msg: String) {
        (planMonthFragment ?: plannerFragment)?.also {
            showSnackbar(it, msg)
        } ?: run { (activity as ProtectedFragmentActivity).showSnackBar(msg) }
    }

    fun showSnackbar(dialogFragment: DialogFragment, msg: String) {
        dialogFragment.dialog?.window?.also {
            val snackbar = Snackbar.make(it.decorView, msg, Snackbar.LENGTH_LONG)
            UiUtils.increaseSnackbarMaxLines(snackbar)
            snackbar.show()
        } ?: run {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun dispatchDeleteDo(tag: LongArray) {
        showSnackbar(getString(R.string.progress_dialog_deleting))
        viewModel.deleteTemplates(tag, PermissionGroup.CALENDAR.hasPermission(requireContext()))
            .observe(
                viewLifecycleOwner
            ) { result: Int ->
                val activity = requireActivity() as BaseActivity
                if (result > 0) {
                    activity.showSnackBar(
                        activity.resources.getQuantityString(
                            R.plurals.delete_success,
                            result,
                            result
                        )
                    )
                } else {
                    activity.showDeleteFailureFeedback(null)
                }
            }
    }

    private fun confirmDeleteTransactionsForPlanInstances(
        planInstances: Array<out PlanInstanceInfo>, dialogTag: String, title: Int
    ) {
        SimpleDialog.build()
            .title(title)
            .extra(Bundle().apply {
                putParcelableArray(KEY_INSTANCES, planInstances)
            })
            .msg(
                concatResStrings(
                    requireContext(),
                    " ",
                    R.string.warning_plan_instance_delete,
                    R.string.continue_confirmation
                )
            )
            .pos(R.string.response_yes)
            .neg(R.string.response_no)
            .show(this, dialogTag)
    }

    private fun dispatchCancelInstance(vararg planInstances: PlanInstanceInfo) {
        val countInstantiated = planInstances.count { planInstanceInfo -> planInstanceInfo.transactionId?.takeIf { it != 0L } != null }
        if (countInstantiated > 0) {
            confirmDeleteTransactionsForPlanInstances(
                planInstances,
                DIALOG_TAG_CONFIRM_CANCEL,
                R.string.menu_cancel_plan_instance
            )
        } else {
            viewModel.cancel(planInstances)
        }
    }

    private fun dispatchResetInstance(vararg planInstances: PlanInstanceInfo) {
        val countInstantiated = planInstances.count { planInstanceInfo -> planInstanceInfo.transactionId?.takeIf { it != 0L } != null }
        if (countInstantiated > 0) {
            confirmDeleteTransactionsForPlanInstances(
                planInstances,
                DIALOG_TAG_CONFIRM_RESET,
                R.string.menu_reset_plan_instance
            )
        } else {
            viewModel.reset(planInstances)
        }
    }

    private class RepairHandler(fragment: TemplatesList) : Handler() {
        private val mFragment: WeakReference<TemplatesList> = WeakReference(fragment)
        override fun handleMessage(msg: Message) {
            val missingUuids = msg.obj as Array<String>
            mFragment.get()?.let { fragment ->
                fragment.activity?.let {
                    fragment.repairTriggered = true
                    (it as ProtectedFragmentActivity).startTaskExecution(
                        TaskExecutionFragment.TASK_REPAIR_PLAN,
                        missingUuids,
                        null,
                        0
                    )
                }
            }

        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        if (loader.id == SORTABLE_CURSOR) {
            mTemplatesCursor = null
            mAdapter!!.swapCursor(null)
        }
    }

    override fun getSortOrderPrefKey(): PrefKey {
        return PrefKey.SORT_ORDER_TEMPLATES
    }

    //after orientation change, we need to restore the reference
    private val planMonthFragment: PlanMonthFragment?
        get() = childFragmentManager.findFragmentByTag(CALDROID_DIALOG_FRAGMENT_TAG) as PlanMonthFragment?
    private val plannerFragment: PlannerFragment?
        get() = childFragmentManager.findFragmentByTag(PLANNER_FRAGMENT_TAG) as PlannerFragment?

    private inner class MyAdapter(
        context: Context, layout: Int, c: Cursor?, from: Array<String>,
        to: IntArray?, flags: Int
    ) : SimpleCursorAdapter(context, layout, c, from, to, flags) {
        private val colorExpense: Int =
            ResourcesCompat.getColor(resources, R.color.colorExpense, null)
        private val colorIncome: Int =
            ResourcesCompat.getColor(resources, R.color.colorIncome, null)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val convertView = super.getView(position, convertView, parent)
            val c = cursor
            c.moveToPosition(position)
            val isSealed = c.getInt(columnIndexIsSealed) != 0
            val doesHavePlan = !c.isNull(columnIndexPlanId)
            val tv1 = convertView.findViewById<TextView>(R.id.amount)
            val amount = c.getLong(columnIndexAmount)
            tv1.setTextColor(if (amount < 0) colorExpense else colorIncome)
            tv1.text = currencyFormatter.convAmount(
                amount,
                currencyContext[c.getString(columnIndexCurrency)]
            )
            val color = c.getInt(columnIndexColor)
            convertView.findViewById<View>(R.id.colorAccount).setBackgroundColor(color)
            val tv2 = convertView.findViewById<TextView>(R.id.category)
            var catText = tv2.text
            if (!c.isNull(columnIndexTransferAccount)) {
                catText = Transfer.getIndicatorPrefixForLabel(amount) + catText
            } else {
                val catId = DbUtils.getLongOrNull(c, DatabaseConstants.KEY_CATID)
                if (catId == null) {
                    catText = Category.NO_CATEGORY_ASSIGNED_LABEL
                }
            }
            //TODO: simplify confer TemplateWidget
            var ssb: SpannableStringBuilder
            val comment = c.getString(columnIndexComment)
            val commentSeparator = " / "
            if (comment != null && comment.isNotEmpty()) {
                ssb = SpannableStringBuilder(comment)
                ssb.setSpan(StyleSpan(Typeface.ITALIC), 0, comment.length, 0)
                catText = TextUtils.concat(catText, commentSeparator, ssb)
            }
            val payee = c.getString(columnIndexPayee)
            if (payee != null && payee.isNotEmpty()) {
                ssb = SpannableStringBuilder(payee)
                ssb.setSpan(UnderlineSpan(), 0, payee.length, 0)
                catText = TextUtils.concat(catText, commentSeparator, ssb)
            }
            tv2.text = catText
            if (doesHavePlan) {
                var planInfo: CharSequence? =
                    if (columnIndexPlanInfo == -1) null else c.getString(columnIndexPlanInfo)
                if (planInfo == null) {
                    planInfo = if (isCalendarPermissionGranted) {
                        getString(R.string.plan_event_deleted)
                    } else {
                        Utils.getTextWithAppName(context, R.string.calendar_permission_required)
                    }
                }
                (convertView.findViewById<View>(R.id.title) as TextView).text =
                    "${c.getString(columnIndexTitle)} ($planInfo)"
            }
            val planImage = convertView.findViewById<ImageView>(R.id.Plan)
            planImage.setImageResource(
                if (isSealed) R.drawable.ic_lock else if (doesHavePlan) R.drawable.ic_event else R.drawable.ic_menu_template
            )
            planImage.contentDescription =
                getString(if (doesHavePlan) R.string.plan else R.string.template)
            return convertView
        }

    }

    override fun configureMenu(menu: Menu, lv: AbsListView) {
        super.configureMenu(menu, lv)
        val id = lv.id
        if (id == R.id.list) {
            val checkedItemPositions = binding.list.checkedItemPositions
            var hasForeignExchangeTransfer = false
            var hasPlan = false
            var hasSealed = false
            for (i in 0 until checkedItemPositions.size()) {
                if (checkedItemPositions.valueAt(i) && isForeignExchangeTransfer(
                        checkedItemPositions.keyAt(i)
                    )
                ) {
                    hasForeignExchangeTransfer = true
                    break
                }
            }
            for (i in 0 until checkedItemPositions.size()) {
                if (checkedItemPositions.valueAt(i) && isPlan(checkedItemPositions.keyAt(i))) {
                    hasPlan = true
                    break
                }
            }
            for (i in 0 until checkedItemPositions.size()) {
                if (checkedItemPositions.valueAt(i) && isSealed(checkedItemPositions.keyAt(i))) {
                    hasSealed = true
                    break
                }
            }
            configureMenuInternal(
                menu,
                lv.checkedItemCount,
                hasForeignExchangeTransfer,
                hasPlan,
                hasSealed
            )
        }
    }

    private fun configureMenuInternal(
        menu: Menu,
        count: Int,
        foreignExchangeTransfer: Boolean,
        hasPlan: Boolean,
        hasSealed: Boolean
    ) {
        menu.findItem(R.id.CREATE_INSTANCE_SAVE_COMMAND).isVisible =
            !foreignExchangeTransfer && !hasPlan and !hasSealed
        menu.findItem(R.id.CREATE_INSTANCE_EDIT_COMMAND).isVisible =
            count == 1 && !hasPlan && !hasSealed
        menu.findItem(R.id.DEFAULT_ACTION_MENU).isVisible = !hasPlan
        menu.findItem(R.id.EDIT_COMMAND).isVisible = count == 1 && !hasSealed
    }

    private fun isForeignExchangeTransfer(position: Int): Boolean {
        if (mTemplatesCursor != null && mTemplatesCursor!!.moveToPosition(position)) {
            if (!mTemplatesCursor!!.isNull(columnIndexTransferAccount)) {
                val transferAccount = Account.getInstanceFromDb(
                    mTemplatesCursor!!.getLong(columnIndexTransferAccount)
                )
                return mTemplatesCursor!!.getString(columnIndexCurrency) != transferAccount.currencyUnit.code
            }
        }
        return false
    }

    private fun isPlan(position: Int): Boolean {
        return if (mTemplatesCursor != null && mTemplatesCursor!!.moveToPosition(position)) {
            !mTemplatesCursor!!.isNull(columnIndexPlanId)
        } else false
    }

    private fun isSealed(position: Int): Boolean {
        return if (mTemplatesCursor != null && mTemplatesCursor!!.moveToPosition(position)) {
            mTemplatesCursor!!.getInt(columnIndexIsSealed) == 1
        } else false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.templates, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.PLANNER_COMMAND)
        if (menuItem != null) {
            Utils.menuItemSetEnabledAndVisible(menuItem, hasPlans)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.PLANNER_COMMAND) {
            PlannerFragment().show(childFragmentManager, PLANNER_FRAGMENT_TAG)
            return true
        }
        return handleSortOption(item)
    }

    override fun inflateContextualActionBar(menu: Menu, listId: Int) {
        if (listId == R.id.list) {
            super.inflateContextualActionBar(menu, listId)
        } else if (listId == R.id.calendar_gridview) {
            requireActivity().menuInflater.inflate(R.menu.planlist_context, menu)
        }
    }

    public override fun loadData() {
        Utils.requireLoader(mManager, SORTABLE_CURSOR, null, this)
    }

    companion object {
        private const val SORTABLE_CURSOR = -1
        const val CALDROID_DIALOG_FRAGMENT_TAG = "CALDROID_DIALOG_FRAGMENT"
        const val PLANNER_FRAGMENT_TAG = "PLANNER_FRAGMENT"
    }

    @Suppress("UNCHECKED_CAST")
    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_TAG_CONFIRM_RESET -> viewModel.reset(extras.getParcelableArray(KEY_INSTANCES) as Array<PlanInstanceInfo>)
                DIALOG_TAG_CONFIRM_CANCEL -> viewModel.cancel(
                    extras.getParcelableArray(
                        KEY_INSTANCES
                    ) as Array<PlanInstanceInfo>
                )
            }
        }
        return true
    }

    fun configureOnClickPopup(
        view: View,
        planInstance: PlanInstanceInfo,
        onClick: (() -> Boolean)?,
        handleMenuItemClick: ((Int) -> Boolean)?
    ) {
        view.setOnClickListener {
            if (popup != null) {
                Timber.d("Caught double click")
                return@setOnClickListener
            }
            if (onClick?.invoke() == true) {
                return@setOnClickListener
            }

            popup = PopupMenu(requireContext(), view).apply {
                inflate(R.menu.planlist_context)
                configureMenuInternalPlanInstances(menu, planInstance.state)
                setOnMenuItemClickListener { item ->
                    if (handleMenuItemClick?.invoke(item.itemId) == true) {
                        return@setOnMenuItemClickListener true
                    }
                    when (item.itemId) {
                        R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND -> {
                            dispatchCreateInstanceEdit(
                                planInstance.templateId, planInstance.instanceId!!,
                                planInstance.date!!
                            )
                            true
                        }
                        R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND -> {
                            dispatchCreateInstanceSaveDo(planInstance)
                            true

                        }
                        R.id.EDIT_PLAN_INSTANCE_COMMAND -> {
                            dispatchEditInstance(planInstance.transactionId)
                            true
                        }
                        R.id.CANCEL_PLAN_INSTANCE_COMMAND -> {
                            dispatchCancelInstance(planInstance)
                            true
                        }
                        R.id.RESET_PLAN_INSTANCE_COMMAND -> {
                            dispatchResetInstance(planInstance)
                            true
                        }
                        else -> false
                    }
                }
                setOnDismissListener {
                    popup = null
                }
                //displaying the popup
                show()
            }
        }
    }
}