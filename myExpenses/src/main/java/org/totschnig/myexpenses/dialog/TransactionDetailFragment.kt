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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ViewIntentProvider
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.Icon
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.emToDp
import org.totschnig.myexpenses.compose.size
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.BOOKING_VALUE
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.DATE_TIME
import org.totschnig.myexpenses.util.ui.getBestForeground
import org.totschnig.myexpenses.util.ui.getDateMode
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.Transaction
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class TransactionDetailFragment : ComposeBaseDialogFragment3() {
    private val viewModel: TransactionDetailViewModel by viewModels()

    @Inject
    lateinit var viewIntentProvider: ViewIntentProvider

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    override val fullScreenIfNotLarge: Boolean
        get() = requireArguments().getBoolean(KEY_FULL_SCREEN)

    private val bankingFeature: BankingFeature
        get() = injector.bankingFeature() ?: object : BankingFeature {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
        injector.inject(viewModel)
    }

    override val title: CharSequence
        get() = getString(R.string.loading)

    @Composable
    fun TableRow(
        modifier: Modifier = Modifier,
        @StringRes label: Int,
        content: String,
        color: Color = Color.Unspecified
    ) {
        TableRow(modifier, stringResource(label), content, color)
    }

    @Composable
    fun TableRow(
        modifier: Modifier = Modifier,
        label: String,
        content: String,
        color: Color = Color.Unspecified
    ) {
        TableRow(label) {
            Text(
                modifier = modifier,
                text = content,
                color = color
            )
        }
    }

    @Composable
    fun TableRow(
        label: String,
        content: @Composable () -> Unit
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = label
            )
            Box(modifier = Modifier.weight(2f)) {
                content()
            }
        }
    }


    @Composable
    override fun ColumnScope.MainContent() {
        val rowId = requireArguments().getLong(DatabaseConstants.KEY_ROWID)
        val transactionInfo = viewModel.transaction(rowId).observeAsState()
        transactionInfo.value?.also { info ->
            if (info.isEmpty()) {
                Text(stringResource(R.string.transaction_deleted))
            } else {
                val transaction = info.first()
                val isIncome = transaction.amount.amountMinor > 0
                LaunchedEffect(transaction) {
                    transactionInfo.value?.let {
                        (dialog as? AlertDialog)?.setTitle(
                            when {
                                transaction.isSplit -> getString(R.string.split_transaction)

                                transaction.isTransfer -> getString(R.string.transfer)

                                transaction.status == STATUS_ARCHIVE ->
                                    "${getString(R.string.archive)} (${transaction.comment})"

                                else -> getString(if (isIncome) R.string.income else R.string.expense)
                            }
                        )
                    }
                }

                ExpandedRenderer(transaction)

                if (transaction.isSplit || transaction.isArchive) {

                    HeadingRenderer(
                        stringResource(
                            if (transaction.isSplit) R.string.split_parts_heading
                            else R.string.import_select_transactions
                        )
                    )

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        var selectedArchivedTransaction by mutableLongStateOf(0)
                        items(info.size - 1) { index ->
                            val part = info[index + 1]
                            AnimatedContent(
                                targetState = selectedArchivedTransaction == part.id,
                                label = "ExpandedTransactionCard"
                            ) { expanded ->
                                if (expanded) {
                                    OutlinedCard(modifier = Modifier
                                        .clickable {
                                            selectedArchivedTransaction = 0
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            ExpandedRenderer(part, true)
                                            if (part.isSplit) {
                                                HeadingRenderer(stringResource(R.string.split_parts_heading))
                                                val partInfo =
                                                    viewModel.transaction(part.id).observeAsState()
                                                partInfo.value?.drop(1)?.forEach {
                                                    CondensedRenderer(part = it)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    CondensedRenderer(
                                        Modifier.conditional(transaction.isArchive) {
                                            clickable { selectedArchivedTransaction = part.id }
                                        },
                                        part,
                                        withDate = transaction.isArchive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        ButtonRow {
            TextButton(onClick = { dismiss() }) {
                Text(stringResource(id = android.R.string.ok))
            }
            transactionInfo.value
                ?.get(0)
                ?.takeIf { !(it.crStatus == CrStatus.VOID || it.isSealed || it.isArchive) }
                ?.let { transaction ->
                    TextButton(onClick = {
                        if (transaction.isTransfer && transaction.hasTransferPeerParent) {
                            showSnackBar(R.string.warning_splitpartcategory_context)
                        } else {
                            dismiss()
                            (requireActivity() as BaseActivity).startEdit(
                                Intent(
                                    requireActivity(),
                                    ExpenseEdit::class.java
                                ).apply {
                                    putExtra(DatabaseConstants.KEY_ROWID, transaction.id)
                                }
                            )
                        }
                    }) {
                        Text(stringResource(id = R.string.menu_edit))
                    }
                }
        }
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun ExpandedRenderer(transaction: Transaction, forPart: Boolean = false) {
        val isIncome = transaction.amount.amountMinor > 0
        if (!forPart || transaction.isTransfer) {
            TableRow(
                label = if (transaction.isTransfer) R.string.transfer_from_account else R.string.account,
                content = if (transaction.isTransfer && isIncome)
                    transaction.transferAccount!! else transaction.accountLabel
            )
        }
        if (transaction.isTransfer) {
            TableRow(
                label = R.string.transfer_to_account,
                content = if (isIncome) transaction.accountLabel else transaction.transferAccount!!
            )
        } else if (transaction.catId != null && transaction.catId > 0) {
            TableRow(
                label = R.string.category,
                content = transaction.categoryPath!!
            )
        }
        if (!transaction.isArchive) {
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

            TableRow(
                label = if (dateMode == BOOKING_VALUE) R.string.booking_date else R.string.date,
                content = dateText
            )
            if (dateMode == BOOKING_VALUE) {
                TableRow(
                    label = R.string.value_date,
                    content = epoch2ZonedDateTime(transaction.valueDate)
                        .format(dateFormatter)
                )
            }
        }

        transaction.originalAmount?.let {
            TableRow(
                label = R.string.menu_original_amount,
                content = formatCurrencyAbs(it)
            )
        }
        TableRow(
            label = if (transaction.isArchive) R.string.menu_aggregates else R.string.amount,
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
        if (!transaction.isTransfer && transaction.amount.currencyUnit.code != currencyContext.homeCurrencyUnit.code) {
            TableRow(
                label = R.string.menu_equivalent_amount,
                content = formatCurrencyAbs(transaction.equivalentAmount)
            )
        }
        if (!(transaction.isArchive || transaction.comment.isNullOrBlank())) {
            TableRow(
                label = R.string.comment,
                content = transaction.comment
            )
        }
        if (transaction.payee != "" || transaction.debtLabel != null) {
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
        if (!transaction.methodLabel.isNullOrBlank()) {
            TableRow(
                label = R.string.method,
                content = transaction.methodLabel
            )
        }
        if (!transaction.referenceNumber.isNullOrBlank()) {
            TableRow(
                label = R.string.reference_number,
                content = transaction.referenceNumber
            )
        }
        if (!(transaction.isArchive || transaction.accountType == AccountType.CASH)) {
            val roles = transaction.crStatus.toColorRoles(requireContext())
            TableRow(
                modifier = Modifier.background(color = Color(roles.accent)),
                label = R.string.status,
                content = stringResource(id = transaction.crStatus.toStringRes()),
                color = Color(roles.onAccent)
            )
        }
        if (transaction.originTemplate != null) {
            TableRow(
                label = R.string.plan,
                content = transaction.originTemplate.plan?.let {
                    Plan.prettyTimeInfo(requireContext(), it.rRule, it.dtStart)
                } ?: stringResource(R.string.plan_event_deleted)
            )
        }
        if (transaction.tagList.isNotEmpty()) {
            val interactionSource = remember { NoRippleInteractionSource() }
            TableRow(stringResource(R.string.tags)) {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentEnforcement provides false
                ) {
                    FlowRow(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (tag in transaction.tagList) {
                            SuggestionChip(
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = tag.color?.let { Color(it) }
                                        ?: Color.Unspecified,
                                    labelColor = tag.color?.let {
                                        Color(getBestForeground(it))
                                    } ?: Color.Unspecified
                                ),
                                onClick = { },
                                label = { Text(tag.label) },
                                interactionSource = interactionSource
                            )
                        }
                    }
                }
            }
        }
        viewModel.attachments(transaction.id).observeAsState().value?.let {
            if (it.isNotEmpty()) {
                TableRow(stringResource(R.string.attachments)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        it.forEach { (uri, info) ->
                            val onClick = {
                                viewIntentProvider.startViewAction(
                                    requireActivity(),
                                    uri,
                                    info.type
                                )
                            }
                            when {
                                info.thumbnail != null -> Image(
                                    modifier = Modifier
                                        .clickable(onClick = onClick)
                                        .size(48.dp)
                                        .padding(5.dp),
                                    bitmap = info.thumbnail.asImageBitmap(),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )

                                info.typeIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                                    Image(
                                        modifier = Modifier
                                            .clickable(onClick = onClick)
                                            .size(48.dp),
                                        painter = rememberDrawablePainter(
                                            drawable = info.typeIcon.loadDrawable(
                                                requireContext()
                                            )
                                        ),
                                        contentDescription = null
                                    )

                                else -> Image(
                                    painter = painterResource(
                                        id = info.fallbackResource ?: 0
                                    ), contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
        viewModel.attributes(transaction.id).observeAsState().value?.let { map ->
            map.forEach { entry ->
                HeadingRenderer(entry.key)

                entry.value.filter { it.first.userVisible }.forEach {
                    TableRow(
                        label = (it.first as? FinTsAttribute)?.let { attribute ->
                            bankingFeature.resolveAttributeLabel(requireContext(), attribute)
                        } ?: it.first.name,
                        content = it.second
                    )
                }
            }
        }
    }

    @Composable
    fun HeadingRenderer(
        text: String
    ) {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            text = text
        )
    }

    @Composable
    fun CondensedRenderer(
        modifier: Modifier = Modifier,
        part: Transaction,
        withDate: Boolean = false
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (withDate) {
                Text(
                    modifier = Modifier.width(emToDp(4f)),
                    text = LocalDateFormatter.current.format(part.date),
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            }
            part.icon?.let {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(40.sp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(it)
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = buildAnnotatedString {
                    append(
                        when {
                            part.isTransfer -> Transfer.getIndicatorPrefixForLabel(
                                part.amountRaw
                            ) + part.transferAccount

                            else -> part.categoryPath
                                ?: Category.NO_CATEGORY_ASSIGNED_LABEL
                        }
                    )
                    part.comment.takeIf { !it.isNullOrBlank() }?.let {
                        append(" / ")
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(it)
                        }
                    }
                    part.debtLabel.takeIf { !it.isNullOrBlank() }?.let {
                        append(" / ")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(it)
                        }
                    }
                    part.tagList.takeIf { it.isNotEmpty() }?.let {
                        append(" / ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            it.forEachIndexed { index, tag ->
                                tag.color?.also { color ->
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color(
                                                color
                                            )
                                        )
                                    ) {
                                        append(tag.label)
                                    }
                                } ?: run {
                                    append(tag.label)
                                }
                                if (index < it.size - 1) {
                                    append(", ")
                                }
                            }
                        }
                    }
                })
            ColoredAmountText(money = part.amount)
        }
    }

    private fun formatCurrencyAbs(money: Money?): String {
        return currencyFormatter.formatCurrency(money!!.amountMajor.abs(), money.currencyUnit)
    }

    companion object {
        const val KEY_FULL_SCREEN = "fullScreen"
        fun show(id: Long, fragmentManager: FragmentManager, fullScreen: Boolean = false) {
            with(fragmentManager) {
                if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                    newInstance(id, fullScreen).show(this, TransactionDetailFragment::class.java.name)
                }
            }
        }

        private fun newInstance(id: Long, fullScreen: Boolean): TransactionDetailFragment =
            TransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(DatabaseConstants.KEY_ROWID, id)
                    putBoolean(KEY_FULL_SCREEN, fullScreen)
                }
            }
    }
}

class NoRippleInteractionSource : MutableInteractionSource {

    override val interactions: Flow<Interaction> = emptyFlow()

    override suspend fun emit(interaction: Interaction) {}

    override fun tryEmit(interaction: Interaction) = true

}