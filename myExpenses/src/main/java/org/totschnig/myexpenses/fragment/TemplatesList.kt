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

import android.annotation.SuppressLint
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
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ImageView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.size
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.EDIT_REQUEST
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.COMMENT_SEPARATOR
import org.totschnig.myexpenses.databinding.TemplatesListBinding
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.getCurrencyForAccount
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForTemplatesWithPlans
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transfer.RIGHT_ARROW
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_INFO
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.isNull
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.TemplatesListViewModel
import timber.log.Timber
import java.io.Serializable
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.absoluteValue

const val KEY_INSTANCE = "instance"
const val DIALOG_TAG_CONFIRM_CANCEL = "confirm_cancel"
const val DIALOG_TAG_CONFIRM_RESET = "confirm_reset"
const val DIALOG_TAG_CONFIRM_DELETE = "confirm_delete"

class TemplatesList : SortableListFragment(), LoaderManager.LoaderCallbacks<Cursor?>,
    SimpleDialog.OnDialogResultListener {
    private var popup: PopupMenu? = null
    override val menuResource: Int
        get() = R.menu.templateslist_context
    private var _templatesCursor: Cursor? = null

    val templatesCursor: Cursor
        get() = _templatesCursor!!
    private lateinit var mAdapter: SimpleCursorAdapter
    private lateinit var mManager: LoaderManager
    private var hasPlans = false

    /**
     * if we are called from the calendar app, we only need to handle display of plan once
     */
    @State
    var expandedHandled = false

    @State
    var repairTriggered = false

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var repository: Repository

    val viewModel: TemplatesListViewModel by viewModels()

    private var _binding: TemplatesListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        appComponent.inject(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val ctx = requireActivity() as ProtectedFragmentActivity
        _binding = TemplatesListBinding.inflate(inflater, container, false)
        mManager = LoaderManager.getInstance(this)
        mManager.initLoader(SORTABLE_CURSOR, null, this)
        mAdapter = MyAdapter(ctx)
        binding.list.adapter = mAdapter
        binding.list.emptyView = binding.empty
        binding.list.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, id: Long ->
            if (_templatesCursor == null || !templatesCursor.moveToPosition(position)) return@setOnItemClickListener
            val isSealed = templatesCursor.getInt(KEY_SEALED) != 0
            if (isSealed) {
                ctx.showSnackBar(R.string.object_sealed)
            }
            if (templatesCursor.isNull(KEY_PLANID)) {
                if (!isSealed) {
                    if (isForeignExchangeTransfer(position)) {
                        dispatchCreateInstanceEditDo(id)
                    } else {
                        val splitAtPosition = isSplitAtPosition(position)
                        val defaultAction: Template.Action = enumValueOrDefault(
                            templatesCursor.getString(KEY_DEFAULT_ACTION),
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
                        templatesCursor.getString(KEY_TITLE),
                        id,
                        templatesCursor.getLong(KEY_PLANID),
                        templatesCursor.getInt(KEY_COLOR), isSealed, prefHandler
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
        resultFeedBack: Int,
    ) {
        viewModel.updateDefaultAction(itemIds, action).observe(
            viewLifecycleOwner
        ) { result: Boolean -> showSnackbar(if (result) getString(resultFeedBack) else "Error while setting default action for template click") }
    }

    override fun dispatchCommandMultiple(
        command: Int,
        positions: SparseBooleanArray, itemIds: LongArray,
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
            i.putExtra(KEY_TEMPLATEID, menuInfo.id)
            //TODO check what to do on Result
            startActivityForResult(i, EDIT_REQUEST)
            return true
        }
        return false
    }

    private fun isSplitAtPosition(position: Int) = _templatesCursor?.let {
        it.moveToPosition(position) && it.getLongOrNull(KEY_CATID) == SPLIT_CATID
    } ?: false

    private fun hasSplitAtPositions(positions: SparseBooleanArray): Boolean {
        for (i in 0 until positions.size) {
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
        viewModel.newFromTemplate(*plans).observe(
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
        startActivity(Intent(requireActivity(), ExpenseEdit::class.java).apply {
            action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
            putExtra(KEY_TEMPLATEID, itemId)
        })
    }

    private fun dispatchCreateInstanceEdit(templateId: Long, instanceId: Long, date: Long) {
        startActivity(
            Intent(requireActivity(), ExpenseEdit::class.java)
                .putExtra(KEY_TEMPLATEID, templateId)
                .putExtra(KEY_INSTANCEID, instanceId)
                .putExtra(KEY_DATE, date)
        )
    }

    fun dispatchEditInstance(transactionId: Long?) {
        startActivityForResult(
            Intent(requireActivity(), ExpenseEdit::class.java)
                .putExtra(KEY_ROWID, transactionId), EDIT_REQUEST
        )
    }

    @Deprecated("Deprecated in Java")
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
                    .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_PLAN_INFO)
                    .build(),
                null,
                DatabaseConstants.KEY_PARENTID + " IS NULL",
                null,
                preferredOrderByForTemplatesWithPlans(prefHandler, Sort.USAGES, prefHandler.collate)
            )
        } else throw IllegalArgumentException()
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, c: Cursor?) {
        val ctx = requireActivity() as ManageTemplates
        if (loader.id == SORTABLE_CURSOR) {
            _templatesCursor = c
            invalidateCAB()
            hasPlans = false
            if (isCalendarPermissionGranted && _templatesCursor != null && templatesCursor.moveToFirst()) {
                val needToExpand =
                    if (expandedHandled) ManageTemplates.NOT_CALLED else ctx.calledFromCalendarWithId
                var planMonthFragment: PlanMonthFragment? = null
                while (!templatesCursor.isAfterLast) {
                    val planId = templatesCursor.getLong(KEY_PLANID)
                    if (planId != 0L) {
                        hasPlans = true
                    }
                    val templateId = templatesCursor.getLong(KEY_ROWID)
                    if (needToExpand == templateId) {
                        planMonthFragment = PlanMonthFragment.newInstance(
                            templatesCursor.getString(KEY_TITLE),
                            templateId,
                            planId,
                            templatesCursor.getInt(KEY_COLOR),
                            templatesCursor.getInt(KEY_SEALED) != 0,
                            prefHandler
                        )
                    }
                    templatesCursor.moveToNext()
                }
                if (needToExpand != ManageTemplates.NOT_CALLED) {
                    expandedHandled = true
                    if (planMonthFragment != null) {
                        planMonthFragment.show(childFragmentManager, CALDROID_DIALOG_FRAGMENT_TAG)
                    } else {
                        ctx.showSnackBar(R.string.save_transaction_template_deleted)
                    }
                }
                //look for plans that we could possible relink
                if (!repairTriggered && templatesCursor.moveToFirst()) {
                    val missingUuids = ArrayList<String>()
                    while (!templatesCursor.isAfterLast) {
                        if (!templatesCursor.isNull(KEY_PLANID) &&
                            templatesCursor.isNull(KEY_PLAN_INFO)
                        ) {
                            missingUuids.add(
                                templatesCursor.getString(
                                    templatesCursor.getColumnIndexOrThrow(
                                        DatabaseConstants.KEY_UUID
                                    )
                                )
                            )
                        }
                        templatesCursor.moveToNext()
                    }
                    if (missingUuids.size > 0) {
                        RepairHandler(this).obtainMessage(
                            0, missingUuids.toTypedArray()
                        )
                            .sendToTarget()
                    }
                }
            }
            mAdapter.swapCursor(_templatesCursor)
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
            ) { result ->
                val activity = requireActivity() as BaseActivity
                activity.showSnackBar(
                    buildList {
                        if (result.success > 0) {
                            add(
                                activity.resources.getQuantityString(
                                    R.plurals.delete_success,
                                    result.success,
                                    result.success
                                )
                            )
                        }
                        if (result.failure > 0) {
                            add(activity.deleteFailureMessage(null))
                        }
                    }.joinToString(" ")
                )
            }
    }

    private fun confirmDeleteTransactionsForPlanInstances(
        planInstance: PlanInstanceInfo, dialogTag: String, title: Int,
    ) {
        confirmDeleteTransactionsWithBundle(dialogTag, title) {
            putParcelable(KEY_INSTANCE, planInstance)
        }
    }

    private fun confirmDeleteTransactionsWithBundle(
        dialogTag: String, title: Int, extras: Bundle.() -> Unit,
    ) {
        SimpleDialog.build()
            .title(title)
            .extra(Bundle().apply { this.extras() })
            .msg(
                concatResStrings(
                    requireContext(),
                    R.string.warning_plan_instance_delete,
                    R.string.continue_confirmation
                )
            )
            .pos(R.string.response_yes)
            .neg(R.string.response_no)
            .show(this, dialogTag)
    }


    private fun dispatchCancelInstance(planInstance: PlanInstanceInfo) {
        if (planInstance.transactionId.let { it != null && it != 0L }) {
            confirmDeleteTransactionsForPlanInstances(
                planInstance,
                DIALOG_TAG_CONFIRM_CANCEL,
                R.string.menu_cancel_plan_instance
            )
        } else {
            viewModel.cancel(planInstance)
        }
    }

    private fun dispatchResetInstance(planInstance: PlanInstanceInfo) {
        if (planInstance.transactionId.let { it != null && it != 0L }) {
            confirmDeleteTransactionsForPlanInstances(
                planInstance,
                DIALOG_TAG_CONFIRM_RESET,
                R.string.menu_reset_plan_instance
            )
        } else {
            viewModel.reset(planInstance)
        }
    }

    fun dispatchDeleteInstance(transactionId: Long) {
        confirmDeleteTransactionsWithBundle(DIALOG_TAG_CONFIRM_DELETE, R.string.menu_delete) {
            putLong(KEY_TRANSACTIONID, transactionId)
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
            _templatesCursor = null
            mAdapter.swapCursor(null)
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

    private inner class MyAdapter(context: Context) :
        SimpleCursorAdapter(context, R.layout.template_row, null, emptyArray(), intArrayOf(), 0) {
        private val colorExpense: Int =
            ResourcesCompat.getColor(resources, R.color.colorExpense, null)
        private val colorIncome: Int =
            ResourcesCompat.getColor(resources, R.color.colorIncome, null)
        private val colorTransfer: Int =
            ResourcesCompat.getColor(resources, R.color.colorTransfer, null)

        @SuppressLint("SetTextI18n")
        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val isSealed = cursor.getInt(KEY_SEALED) != 0
            val doesHavePlan = !cursor.isNull(KEY_PLANID)
            val isTransfer = !cursor.isNull(KEY_TRANSFER_ACCOUNT)
            val tv1 = view.findViewById<TextView>(R.id.Amount)
            val amount = cursor.getLong(KEY_AMOUNT)
            val amountColor = when {
                isTransfer -> colorTransfer
                amount < 0 -> colorExpense
                else -> colorIncome
            }
            tv1.setTextColor(amountColor)
            tv1.text = currencyFormatter.convAmount(
                if (isTransfer) amount.absoluteValue else amount,
                currencyContext[cursor.getString(KEY_CURRENCY)]
            )
            val color = cursor.getInt(KEY_COLOR)
            view.findViewById<View>(R.id.colorAccount).setBackgroundColor(color)
            val tv2 = view.findViewById<TextView>(R.id.Category)
            var catText: CharSequence = cursor.getString(KEY_PATH)
            if (isTransfer) {
                val account = cursor.getString(KEY_ACCOUNT_LABEL)
                val transferAccount = cursor.getString(KEY_TRANSFER_ACCOUNT_LABEL)

                val transfer = if (amount < 0) "$account $RIGHT_ARROW $transferAccount"
                else "$transferAccount $RIGHT_ARROW $account"

                catText = if (catText.isEmpty()) transfer
                else TextUtils.concat(catText, " (", transfer, ")")
            }
            //TODO: simplify confer TemplateWidget
            var ssb: SpannableStringBuilder
            val comment = cursor.getStringOrNull(KEY_COMMENT)

            if (!comment.isNullOrEmpty()) {
                ssb = SpannableStringBuilder(comment)
                ssb.setSpan(StyleSpan(Typeface.ITALIC), 0, comment.length, 0)
                catText = if (catText.isEmpty()) ssb else
                    TextUtils.concat(catText, COMMENT_SEPARATOR, ssb)
            }
            val payee = cursor.getStringOrNull(KEY_PAYEE_NAME)
            if (!payee.isNullOrEmpty()) {
                ssb = SpannableStringBuilder(payee)
                ssb.setSpan(UnderlineSpan(), 0, payee.length, 0)
                catText = if (catText.isEmpty()) ssb else
                    TextUtils.concat(catText, COMMENT_SEPARATOR, ssb)
            }
            tv2.text = catText
            view.findViewById<TextView>(R.id.Title).text = cursor.getString(KEY_TITLE) +
                    if (doesHavePlan)
                        " (" + (cursor.getStringIfExists(KEY_PLAN_INFO)
                            ?: if (isCalendarPermissionGranted) {
                                getString(R.string.plan_event_deleted)
                            } else {
                                Utils.getTextWithAppName(
                                    context,
                                    R.string.calendar_permission_required
                                )
                            }) + ")"
                    else ""

            val planImage = view.findViewById<ImageView>(R.id.Plan)
            planImage.setImageResource(
                if (isSealed) R.drawable.ic_lock else if (doesHavePlan) R.drawable.ic_event else R.drawable.ic_menu_template
            )
            planImage.contentDescription =
                getString(if (doesHavePlan) R.string.plan else R.string.template)

            with(view.findViewById<TextView>(R.id.OriginalAmount)) {
                isVisible = cursor.getStringOrNull(KEY_ORIGINAL_CURRENCY)?.let {
                    text = currencyFormatter.convAmount(
                        cursor.getLong(KEY_ORIGINAL_AMOUNT),
                        currencyContext[it]
                    )
                    setTextColor(amountColor)
                    true
                } ?: false
            }
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
            for (i in 0 until checkedItemPositions.size) {
                if (checkedItemPositions.valueAt(i) && isForeignExchangeTransfer(
                        checkedItemPositions.keyAt(i)
                    )
                ) {
                    hasForeignExchangeTransfer = true
                    break
                }
            }
            for (i in 0 until checkedItemPositions.size) {
                if (checkedItemPositions.valueAt(i) && isPlan(checkedItemPositions.keyAt(i))) {
                    hasPlan = true
                    break
                }
            }
            for (i in 0 until checkedItemPositions.size) {
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
        hasSealed: Boolean,
    ) {
        menu.findItem(R.id.CREATE_INSTANCE_SAVE_COMMAND).isVisible =
            !foreignExchangeTransfer && !hasPlan and !hasSealed
        menu.findItem(R.id.CREATE_INSTANCE_EDIT_COMMAND).isVisible =
            count == 1 && !hasPlan && !hasSealed
        menu.findItem(R.id.DEFAULT_ACTION_MENU).isVisible = !hasPlan
        menu.findItem(R.id.EDIT_COMMAND).isVisible = count == 1 && !hasSealed
    }

    private fun isForeignExchangeTransfer(position: Int): Boolean {
        if (_templatesCursor != null && templatesCursor.moveToPosition(position)) {
            if (!templatesCursor.isNull(KEY_TRANSFER_ACCOUNT)) {
                return templatesCursor.getString(KEY_CURRENCY) != repository.getCurrencyForAccount(
                    templatesCursor.getLong(KEY_TRANSFER_ACCOUNT)
                )
            }
        }
        return false
    }

    private fun isPlan(position: Int): Boolean {
        return if (_templatesCursor != null && templatesCursor.moveToPosition(position)) {
            !templatesCursor.isNull(KEY_PLANID)
        } else false
    }

    private fun isSealed(position: Int): Boolean {
        return if (_templatesCursor != null && templatesCursor.moveToPosition(position)) {
            templatesCursor.getInt(KEY_SEALED) == 1
        } else false
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.templates, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.PLANNER_COMMAND)?.setEnabledAndVisible(hasPlans)
    }

    @Deprecated("Deprecated in Java")
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
        } else if (listId == com.caldroid.R.id.calendar_gridview) {
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

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_TAG_CONFIRM_DELETE -> viewModel.deleteTransactions(
                    longArrayOf(extras.getLong(KEY_TRANSACTIONID))
                )

                DIALOG_TAG_CONFIRM_RESET -> viewModel.reset(extras.getParcelable(KEY_INSTANCE)!!)
                DIALOG_TAG_CONFIRM_CANCEL -> viewModel.cancel(extras.getParcelable(KEY_INSTANCE)!!)
            }
        }
        return true
    }

    fun configureOnClickPopup(
        view: View,
        planInstance: PlanInstanceInfo,
        onClick: (() -> Boolean)?,
        handleMenuItemClick: ((Int) -> Boolean)?,
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
                                planInstance.date!! / 1000
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

    fun dispatchRelinkInstance(planInstanceInfo: PlanInstanceInfo, adjustDate: Boolean) {
        viewModel.relink(planInstanceInfo, adjustDate)
    }
}