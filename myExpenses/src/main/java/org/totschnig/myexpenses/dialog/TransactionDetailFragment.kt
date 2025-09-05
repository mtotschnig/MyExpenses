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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ViewIntentProvider
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.COMMENT_SEPARATOR
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.Icon
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.SumDetails
import org.totschnig.myexpenses.compose.TEST_TAG_PART_LIST
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.emToDp
import org.totschnig.myexpenses.compose.filter.FilterCard
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbar
import org.totschnig.myexpenses.compose.size
import org.totschnig.myexpenses.compose.voidMarker
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.BOOKING_VALUE
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode.DATE_TIME
import org.totschnig.myexpenses.util.ui.getBestForeground
import org.totschnig.myexpenses.util.ui.getDateMode
import org.totschnig.myexpenses.viewmodel.LoadResult
import org.totschnig.myexpenses.viewmodel.TransactionDetailViewModel
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

    val sortOrder: String?
        get() = requireArguments().getString(KEY_SORT_ORDER)

    val rowId: Long
        get() = requireArguments().getLong(KEY_ROWID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
        injector.inject(viewModel)
    }

    override val title: CharSequence
        get() = getString(R.string.loading)

    override val horizontalPadding = 0.dp

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
        Row(modifier = Modifier.padding(horizontal = super.horizontalPadding)) {
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

    private val filter by lazy {
        BundleCompat.getParcelable(requireArguments(), KEY_FILTER, Criterion::class.java)
    }

    private val transactionLiveData: LiveData<LoadResult> by lazy {
        viewModel.transaction(rowId)
    }

    private val partsLiveData: LiveData<List<Transaction>> by lazy {
        viewModel.parts(rowId, sortOrder, filter)
    }

    @Composable
    override fun ColumnScope.MainContent() {
        val loadResult = transactionLiveData.observeAsState()
        loadResult.value.let { loadResult ->
            if (loadResult == null) {
                Text(stringResource(R.string.loading))
            } else {
                val transaction = loadResult.transaction
                if (transaction == null) {
                    Text(stringResource(R.string.transaction_deleted))
                } else {
                    val isIncome = transaction.amount.amountMinor > 0
                    LaunchedEffect(transaction) {
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
                    filter?.let {
                        FilterCard(
                            whereFilter = it
                        )
                    }

                    ExpandedRenderer(transaction)

                    if (transaction.isSplit || transaction.isArchive) {

                        HeadingRenderer(
                            stringResource(
                                if (transaction.isSplit) R.string.split_parts_heading
                                else R.string.import_select_transactions
                            )
                        )

                        val parts = partsLiveData.observeAsState(emptyList())

                        LazyColumnWithScrollbar(
                            modifier = Modifier.weight(1f, fill = false),
                            itemsAvailable = parts.value.size,
                            contentPadding = PaddingValues(horizontal = super.horizontalPadding),
                            testTag = TEST_TAG_PART_LIST,
                        ) {
                            var selectedArchivedTransaction by mutableLongStateOf(0)
                            items(parts.value) { part ->
                                AnimatedContent(
                                    targetState = selectedArchivedTransaction == part.id,
                                    label = "ExpandedTransactionCard"
                                ) { expanded ->
                                    if (expanded) {
                                        OutlinedCard(
                                            modifier = Modifier
                                                .voidMarker(part.crStatus)
                                                .clickable {
                                                    selectedArchivedTransaction = 0
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                ExpandedRenderer(part, true)
                                                if (part.isSplit) {
                                                    HeadingRenderer(stringResource(R.string.split_parts_heading))
                                                    viewModel.parts(part.id, sortOrder)
                                                        .observeAsState(emptyList())
                                                        .value.forEach {
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
                                            parentIsArchive = transaction.isArchive
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ButtonRow(modifier = Modifier.padding(horizontal = super.horizontalPadding)) {
            TextButton(onClick = { dismiss() }) {
                Text(stringResource(id = android.R.string.ok))
            }
            loadResult.value?.transaction
                ?.takeIf { !(it.crStatus == CrStatus.VOID || it.isSealed || it.isArchive) }
                ?.let { transaction ->
                    TextButton(onClick = {
                        if (transaction.isTransfer && transaction.transferPeerIsPart) {
                            showSnackBar(
                                if (transaction.transferPeerIsArchived) R.string.warning_archived_transfer_cannot_be_edited else R.string.warning_splitpartcategory_context
                            )
                        } else {
                            dismiss()
                            (requireActivity() as BaseActivity).startEdit(
                                Intent(
                                    requireActivity(),
                                    ExpenseEdit::class.java
                                ).apply {
                                    putExtra(KEY_ROWID, transaction.id)
                                }
                            )
                        }
                    }) {
                        Text(stringResource(id = R.string.menu_edit))
                    }
                }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
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
        }
        if (transaction.catId != null && transaction.catId > 0) {
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
                content = formatCurrency(it, true)
            )
        }
        TableRow(
            label = if (transaction.isArchive) R.string.menu_aggregates else R.string.amount,
            content = if (transaction.isTransfer) {
                if (transaction.isSameCurrency) {
                    formatCurrency(transaction.amount, true)
                } else {
                    val self = formatCurrency(transaction.amount, true)
                    val other = formatCurrency(transaction.transferAmount!!, true)
                    if (isIncome) "$other => $self" else "$self => $other"
                }
            } else {
                formatCurrency(transaction.amount, !transaction.isArchive)
            }
        )
        if (transaction.isArchive) {
            viewModel.sums(transaction.id).observeAsState().value?.let { sums ->
                SumDetails(
                    Money(transaction.amount.currencyUnit, sums.first),
                    Money(transaction.amount.currencyUnit, sums.second),
                    Money(transaction.amount.currencyUnit, sums.third),
                    alignStart = true,
                    modifier = Modifier.padding(horizontal = super.horizontalPadding)
                )
            }
        }
        if (!transaction.isTransfer && transaction.isForeign) {
            TableRow(
                label = R.string.menu_equivalent_amount,
                content = formatCurrency(transaction.equivalentAmount!!, true)
            )
        }
        if (!(transaction.isArchive || transaction.comment.isNullOrBlank())) {
            TableRow(
                label = R.string.comment,
                content = transaction.comment
            )
        }
        if (transaction.party != null || transaction.debtLabel != null) {
            TableRow(
                label = when {
                    transaction.party == null -> R.string.debt
                    isIncome -> R.string.payer
                    else -> R.string.payee
                },
                content = buildString {
                    transaction.party?.let {
                        append(it.displayName)
                    }
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
        if (!transaction.isArchive && transaction.accountType?.supportsReconciliation == true) {
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
                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified
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
        val attachments = remember { viewModel.attachments(transaction.id) }
        attachments.observeAsState().value?.let {
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
                                    modifier = Modifier
                                        .clickable(onClick = onClick),
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
        val attributes = remember { viewModel.attributes(transaction.id) }
        attributes.observeAsState().value?.let { map ->
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
            modifier = Modifier.padding(vertical = 8.dp, horizontal = super.horizontalPadding),
            style = MaterialTheme.typography.titleMedium,
            text = text
        )
    }

    @Composable
    fun CondensedRenderer(
        modifier: Modifier = Modifier,
        part: Transaction,
        parentIsArchive: Boolean = false
    ) {
        Row(
            modifier = modifier.voidMarker(part.crStatus),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (parentIsArchive) {
                Text(
                    modifier = Modifier.width(emToDp(4.6f)),
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
                    var isNotEmpty = false
                    when {
                        part.isTransfer -> Transfer.getIndicatorPrefixForLabel(
                            part.amountRaw
                        ) + part.transferAccount
                        part.isSplit -> getString(R.string.split_transaction)
                        else -> part.categoryPath
                    }?.let {
                        append(it)
                        isNotEmpty = true
                    }
                    part.comment.takeIf { !it.isNullOrBlank() }?.let {
                        if (isNotEmpty) {
                            append(COMMENT_SEPARATOR)
                        }
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(it)
                            isNotEmpty = true
                        }
                    }
                    if (parentIsArchive) {
                        part.party?.let {
                            if (length > 0) {
                                append(COMMENT_SEPARATOR)
                            }
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(it.displayName)
                            }
                        }
                    } else {
                        part.debtLabel.takeIf { !it.isNullOrBlank() }?.let {
                            if (isNotEmpty) {
                                append(COMMENT_SEPARATOR)
                            }
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(it)
                                isNotEmpty = true
                            }
                        }
                    }

                    part.tagList.takeIf { it.isNotEmpty() }?.let {
                        if (isNotEmpty) {
                            append(COMMENT_SEPARATOR)
                        }
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
            if (part.isForeign) {
                Column(horizontalAlignment = Alignment.End) {
                    ColoredAmountText(part.amount, part.type)
                    ColoredAmountText(part.equivalentAmount!!, part.type)
                }
            } else {
                ColoredAmountText(part.amount, part.type)
            }
        }
    }

    @Composable
    fun ColoredAmountText(
        amount: Money,
        type: Byte
    ) {
        val colorSource = viewModel.colorSource.collectAsStateWithLifecycle(ColorSource.TYPE).value
        ColoredAmountText(
            money = amount,
            type = colorSource.transformType(type)
        )
    }

    private fun formatCurrency(money: Money, abs: Boolean): String {
        return currencyFormatter.formatCurrency(
            money.amountMajor.let { if (abs) it.abs() else it },
            money.currencyUnit
        )
    }

    private val Transaction.isForeign
        get() = amount.currencyUnit.code != currencyContext.homeCurrencyUnit.code

    companion object {
        const val KEY_FULL_SCREEN = "fullScreen"
        const val KEY_SORT_ORDER = "sortOrder"

        fun show(
            id: Long,
            fragmentManager: FragmentManager,
            fullScreen: Boolean = false,
            currentFilter: FilterPersistence? = null,
            sortOrder: String? = null
        ) {
            with(fragmentManager) {
                if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                    newInstance(id, fullScreen, currentFilter, sortOrder)
                        .show(this, TransactionDetailFragment::class.java.name)
                }
            }
        }

        private fun newInstance(
            id: Long,
            fullScreen: Boolean,
            currentFilter: FilterPersistence?,
            sortOrder: String?
        ): TransactionDetailFragment =
            TransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(KEY_ROWID, id)
                    currentFilter?.whereFilter?.value?.let {
                        putParcelable(KEY_FILTER, it)
                    }
                    sortOrder?.let {
                        putString(KEY_SORT_ORDER, it)
                    }
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