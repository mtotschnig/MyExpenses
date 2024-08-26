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

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.widget.TableRow
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ViewIntentProvider
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.BOOKING_VALUE
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.DATE_TIME
import org.totschnig.myexpenses.util.ui.attachmentInfoMap
import org.totschnig.myexpenses.util.ui.getDateMode
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel
import org.totschnig.myexpenses.viewmodel.data.AttachmentInfo
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class TransactionDetailFragment : ComposeBaseDialogFragment3(),
    DialogInterface.OnClickListener {
    private val viewModel: TransactionDetailViewModel by viewModels()

    @Inject
    lateinit var viewIntentProvider: ViewIntentProvider

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    private var attachmentInfoMap: Map<Uri, AttachmentInfo>? = null

    private val bankingFeature: BankingFeature
        get() = injector.bankingFeature() ?: object : BankingFeature {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
        injector.inject(viewModel)
        attachmentInfoMap = attachmentInfoMap(requireContext())
    }

    override val title: CharSequence
        get() = "TODO"

    @Composable
    fun TableRow(
        modifier: Modifier = Modifier,
        @StringRes label: Int,
        content: String,
        color: Color = Color.Unspecified
    ) {
        TableRow(label) {
            Text(
                modifier = it.then(modifier),
                text = content,
                color = color
            )
        }
    }
    
    @Composable
    fun TableRow(
        @StringRes label: Int,
        content: @Composable RowScope.(Modifier) -> Unit
    ) {
        Row {
            Text(modifier = Modifier.weight(1f), text = stringResource(label))
            content(Modifier.weight(2f))
        }
    }
    

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun ColumnScope.MainContent() {
        val rowId = requireArguments().getLong(DatabaseConstants.KEY_ROWID)
        val transactionInfo = viewModel.transaction(rowId).observeAsState()
        transactionInfo.value?.also { info ->
            if (info.isEmpty()) {
                Text(getString(R.string.transaction_deleted))
            } else {
                val transaction = info.first()
                val isIncome = transaction.amount.amountMinor > 0
                LazyColumn {
                    item {
                        TableRow(
                            label = if (transaction.isTransfer) R.string.transfer_from_account else R.string.account,
                            content = if (transaction.isTransfer && isIncome)
                                transaction.transferAccount!! else transaction.accountLabel
                        )
                    }
                    if (transaction.isTransfer) {
                        item {
                            TableRow(
                                label = R.string.transfer_to_account,
                                content = if (isIncome) transaction.accountLabel else transaction.transferAccount!!
                            )
                        }
                    } else if (transaction.catId != null && transaction.catId > 0) {
                        item {
                            TableRow(
                                label = R.string.category,
                                content = transaction.categoryPath!!
                            )
                        }
                    }
                    val dateMode = getDateMode(transaction.accountType, prefHandler)
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

                    val dateText = buildString {
                        append(transaction.date.format(dateFormatter))
                        if (dateMode == DATE_TIME) {
                            append(" ")
                            append(transaction.date.format(timeFormatter))
                        }
                    }

                    item {
                        TableRow(
                            label = if (dateMode == BOOKING_VALUE) R.string.booking_date else R.string.date,
                            content = dateText
                        )
                    }
                    if (dateMode == BOOKING_VALUE) {
                        item {
                            TableRow(
                                label = R.string.value_date,
                                content = epoch2ZonedDateTime(transaction.valueDate)
                                    .format(dateFormatter)
                            )
                        }
                    }
                    transaction.originalAmount?.let {
                        item {
                            TableRow(
                                label = R.string.menu_original_amount,
                                content = formatCurrencyAbs(it)
                            )
                        }
                    }
                    item {
                        TableRow(
                            label = R.string.amount,
                            content = if (transaction.isTransfer) {
                                if (transaction.isSameCurrency) {
                                    formatCurrencyAbs(transaction.amount)
                                } else {
                                    val self = formatCurrencyAbs(transaction.amount)
                                    val other = formatCurrencyAbs(transaction.transferAmount)
                                    if (isIncome) "$other => $self" else "$self => $other"
                                }
                            } else {
                                formatCurrencyAbs(transaction.amount)
                            }
                        )
                    }
                    if (!transaction.isTransfer && transaction.amount.currencyUnit.code != currencyContext.homeCurrencyUnit.code) {
                        item {
                            TableRow(
                                label = R.string.menu_equivalent_amount,
                                content = formatCurrencyAbs(transaction.equivalentAmount)
                            )
                        }
                    }
                    if (!transaction.comment.isNullOrBlank()) {
                        item {
                            TableRow(
                                label = R.string.comment,
                                content = transaction.comment
                            )
                        }
                    }
                    if (transaction.payee != "" || transaction.debtLabel != null) {
                        item {
                            TableRow(
                                label = when {
                                    transaction.payee == "" -> R.string.debt
                                    isIncome -> R.string.payer
                                    else -> R.string.payee
                                },
                                content = buildString {
                                    append(transaction.payee)
                                    transaction.debtLabel?.let {
                                        append(" ($it)")
                                    }
                                    transaction.iban?.let {
                                        append(" (${transaction.iban})")
                                    }
                                }
                            )
                        }
                    }
                    if (!transaction.methodLabel.isNullOrBlank()) {
                        item {
                            TableRow(
                                label = R.string.method,
                                content = transaction.methodLabel
                            )
                        }
                    }
                    if (!transaction.referenceNumber.isNullOrBlank()) {
                        item {
                            TableRow(
                                label = R.string.reference_number,
                                content = transaction.referenceNumber
                            )
                        }
                    }
                    if (transaction.accountType != AccountType.CASH) {
                        val roles = transaction.crStatus.toColorRoles(requireContext())
                        item {
                            TableRow(
                                modifier = Modifier.background(color = Color(roles.accent)),
                                label = R.string.status,
                                content = stringResource(id = transaction.crStatus.toStringRes()),
                                color = Color(roles.onAccent)
                            )
                        }
                    }
                    if (transaction.originTemplate != null) {
                        item {
                            TableRow(
                                label = R.string.plan,
                                content = transaction.originTemplate.plan?.let {
                                    Plan.prettyTimeInfo(requireContext(), it.rRule, it.dtStart)
                                } ?: getString(R.string.plan_event_deleted)
                            )
                        }
                    }
                    if (transaction.tagList.isNotEmpty()) {
                        item { 
                            TableRow(label = R.string.tags) {
                                FlowRow(modifier = it) {
                                    for (tag in transaction.tagList) {
                                        FilterChip(
                                            selected = true,
                                            onClick = {  },
                                            label = { Text(tag.label) })
                                    }
                                }
                            }
                        }
                    }
                    if (transaction.isSplit) {
                        items(info.size - 1) {
                            val part = info[it + 1]
                            Text(part.accountLabel)
                        }
                    }
                }
            }
        } ?: run {
            Text("Loading")
        }
    }
    /*
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = initBuilder {
                TransactionDetailBinding.inflate(it)
            }

            injector.inject(viewModel)
            val rowId = requireArguments().getLong(DatabaseConstants.KEY_ROWID)
            viewModel.transaction(rowId).observe(this) { o -> fillData(o) }
            viewModel.attachments(rowId).observe(this) { attachments ->
                if (attachments.isEmpty()) {
                    binding.AttachmentsRow.visibility = View.GONE
                } else {
                    attachments.forEach { uri ->
                        AttachmentItemBinding.inflate(
                            layoutInflater,
                            binding.AttachmentGroup,
                            false
                        ).root.apply {
                            binding.AttachmentGroup.addView(this)
                            lifecycleScope.launch {
                                val info = withContext(Dispatchers.IO) {
                                    attachmentInfoMap!!.getValue(uri)
                                }
                                setAttachmentInfo(info)
                                setOnClickListener {
                                    viewIntentProvider.startViewAction(
                                        requireActivity(),
                                        uri,
                                        info.type
                                    )
                                }
                            }
                        }
                    }
                }
            }

            viewModel.attributes(rowId).observe(this) { groups ->
                groups.forEach { entry ->
                    binding.OneExpense.addView(
                        AttributeGroupHeaderBinding.inflate(layoutInflater).root.also {
                            it.text = entry.key
                        }, binding.OneExpense.childCount - 1
                    )
                    val attributeTable = AttributeGroupTableBinding.inflate(layoutInflater).root.also {
                        binding.OneExpense.addView(it, binding.OneExpense.childCount - 1)
                    }
                    entry.value.filter { it.first.userVisible }.forEach {
                        attributeTable.addView(
                            with(AttributeBinding.inflate(layoutInflater)) {
                                Name.text = (it.first as? FinTsAttribute)?.let {
                                    bankingFeature.resolveAttributeLabel(
                                        requireContext(),
                                        it
                                    )
                                } ?: it.first.name
                                Value.text = it.second
                                root
                            }
                        )
                    }
                }
            }

            val alertDialog =
                builder.setTitle(R.string.loading) //.setIcon(android.R.color.transparent)
                    .setNegativeButton(android.R.string.ok, this)
                    .setPositiveButton(R.string.menu_edit, null)
                    .create()
            alertDialog.setOnShowListener(object : ButtonOnShowDisabler() {
                override fun onShow(dialog: DialogInterface) {
                    if (transactionData == null) {
                        super.onShow(dialog)
                    }
                    //prevent automatic dismiss on button click
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener { onClick(alertDialog, AlertDialog.BUTTON_POSITIVE) }
                }
            })
            return alertDialog
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            val ctx: FragmentActivity = activity ?: return
            transactionData?.takeIf { it.isNotEmpty() }?.let { list ->
                val transaction = list[0]
                when (which) {
                    AlertDialog.BUTTON_POSITIVE -> {
                        if (transaction.isTransfer && transaction.hasTransferPeerParent) {
                            showSnackBar(R.string.warning_splitpartcategory_context)
                            return
                        }
                        dismissAllowingStateLoss()
                        val i = Intent(ctx, ExpenseEdit::class.java)
                        i.putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
                        ctx.startActivityForResult(i, EDIT_REQUEST)
                    }
                }
            }
        }

        private fun fillData(list: List<Transaction>) {
            transactionData = list
            (dialog as? AlertDialog)?.let { dlg ->
                if (list.isNotEmpty()) {
                    val transaction = list[0]
                    binding.progress.visibility = View.GONE
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                        if (transaction.crStatus == CrStatus.VOID || transaction.isSealed) {
                            it.visibility = View.GONE
                        } else {
                            it.isEnabled = true
                        }
                    }
                    binding.Table.visibility = View.VISIBLE
                    val title: Int
                    val isIncome = transaction.amount.amountMinor > 0
                    when {
                        transaction.isSplit || transaction.status == DatabaseConstants.STATUS_ARCHIVE -> {
                            binding.splitListHeader.visibility = View.VISIBLE
                            binding.splitList.visibility = View.VISIBLE
                            binding.splitList.layoutManager = LinearLayoutManager(requireContext())
                            title = R.string.split_transaction
                            SplitPartRVAdapter(
                                requireContext(),
                                transaction.amount.currencyUnit,
                                currencyFormatter
                            ).also {
                                it.submitList(list.subList(1, list.size))
                                binding.splitList.adapter = it
                            }
                        }

                        transaction.isTransfer -> {
                            title = R.string.transfer
                            binding.AccountLabel.setText(R.string.transfer_from_account)
                            binding.CategoryLabel.setText(R.string.transfer_to_account)
                        }

                        else -> {
                            title = if (isIncome) R.string.income else R.string.expense
                        }
                    }
                    val amountText: String
                    if (transaction.isTransfer) {
                        binding.Account.text =
                            if (isIncome) transaction.transferAccount else transaction.accountLabel
                        binding.Category.text =
                            if (isIncome) transaction.accountLabel else transaction.transferAccount
                        amountText = if (transaction.isSameCurrency) {
                            formatCurrencyAbs(transaction.amount)
                        } else {
                            val self = formatCurrencyAbs(transaction.amount)
                            val other = formatCurrencyAbs(transaction.transferAmount)
                            if (isIncome) "$other => $self" else "$self => $other"
                        }
                    } else {
                        binding.Account.text = transaction.accountLabel
                        if (transaction.catId != null && transaction.catId > 0) {
                            binding.Category.text = transaction.categoryPath
                        } else {
                            binding.CategoryRow.visibility = View.GONE
                        }
                        amountText = formatCurrencyAbs(transaction.amount)
                    }
                    binding.Amount.text = amountText
                    transaction.originalAmount?.let {
                        binding.OriginalAmountRow.visibility = View.VISIBLE
                        binding.OriginalAmount.text = formatCurrencyAbs(it)
                    }
                    if (!transaction.isTransfer && transaction.amount.currencyUnit.code != currencyContext.homeCurrencyUnit.code) {
                        binding.EquivalentAmountRow.visibility = View.VISIBLE
                        binding.EquivalentAmount.text = formatCurrencyAbs(transaction.equivalentAmount)
                    }
                    val dateMode = getDateMode(transaction.accountType, prefHandler)
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    if (dateMode == DateMode.BOOKING_VALUE) {
                        binding.DateLabel.setText(R.string.booking_date)
                        binding.Date2Row.visibility = View.VISIBLE
                        binding.Date2.text = ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(transaction.valueDate),
                            ZoneId.systemDefault()
                        ).format(dateFormatter)
                    }

                    var dateText = transaction.date.format(dateFormatter)
                    if (dateMode == DateMode.DATE_TIME) {
                        dateText += " " + transaction.date.format(timeFormatter)
                    }
                    binding.Date.text = dateText
                    if (transaction.comment.isNullOrBlank()) {
                        binding.CommentRow.visibility = View.GONE
                    } else {
                        binding.Comment.text = transaction.comment
                    }
                    if (transaction.referenceNumber.isNullOrBlank()) {
                        binding.NumberRow.visibility = View.GONE
                    } else {
                        binding.Number.text = transaction.referenceNumber
                    }
                    if (transaction.payee != "" || transaction.debtLabel != null) {
                        val payeeInfo = transaction.payee +
                                (if (transaction.debtLabel == null) "" else " (${transaction.debtLabel})") +
                                if (transaction.iban == null) "" else " (${transaction.iban})"
                        binding.Payee.text = payeeInfo
                        binding.PayeeLabel.setText(
                            when {
                                transaction.payee == "" -> R.string.debt
                                isIncome -> R.string.payer
                                else -> R.string.payee
                            }
                        )
                    } else {
                        binding.PayeeRow.visibility = View.GONE
                    }
                    if (transaction.methodLabel.isNullOrBlank()) {
                        binding.MethodRow.visibility = View.GONE
                    } else {
                        binding.Method.text = transaction.methodLabel
                    }

                    if (transaction.accountType == AccountType.CASH) {
                        binding.StatusRow.visibility = View.GONE
                    } else {
                        val roles = transaction.crStatus.toColorRoles(requireContext())
                        binding.Status.setBackgroundColor(roles.accent)
                        binding.Status.setTextColor(roles.onAccent)
                        binding.Status.setText(transaction.crStatus.toStringRes())
                    }
                    if (transaction.originTemplate == null) {
                        binding.PlanRow.visibility = View.GONE
                    } else {
                        binding.Plan.text = transaction.originTemplate.plan?.let {
                            Plan.prettyTimeInfo(requireContext(), it.rRule, it.dtStart)
                        } ?: getString(R.string.plan_event_deleted)
                    }

                    if (transaction.tagList.isNotEmpty()) {
                        binding.TagGroup.addChipsBulk(transaction.tagList)
                    } else {
                        binding.TagRow.visibility = View.GONE
                    }

                    dlg.setTitle(title)
                } else {
                    binding.error.visibility = View.VISIBLE
                    binding.error.setText(R.string.transaction_deleted)
                }
            }
        }*/

    private fun formatCurrencyAbs(money: Money?): String {
        return currencyFormatter.formatCurrency(money!!.amountMajor.abs(), money.currencyUnit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentInfoMap = null
    }

    companion object {
        fun show(id: Long, fragmentManager: FragmentManager) {
            with(fragmentManager) {
                if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                    newInstance(id).show(this, TransactionDetailFragment::class.java.name)
                }
            }
        }

        private fun newInstance(id: Long): TransactionDetailFragment =
            TransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(DatabaseConstants.KEY_ROWID, id)
                }
            }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        TODO("Not yet implemented")
    }
}