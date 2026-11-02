package org.totschnig.myexpenses.next.search

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.ChoiceChips
import org.totschnig.myexpenses.designsystem.DateDialog
import org.totschnig.myexpenses.designsystem.Dot
import org.totschnig.myexpenses.designsystem.FilterSheet
import org.totschnig.myexpenses.designsystem.LoadingRow
import org.totschnig.myexpenses.designsystem.OptionRow
import org.totschnig.myexpenses.designsystem.SearchField
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.designsystem.SwitchRow
import org.totschnig.myexpenses.designsystem.ValueRow
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.formatRange
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter
import org.totschnig.myexpenses.next.core.ui.rememberMonthFormatter
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TransferCriterion
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import java.time.LocalDate
import java.time.format.FormatStyle

/** Kinds whose values are picked from a list */
private val listKinds = setOf(
    SearchKind.Category, SearchKind.Payee, SearchKind.Tag,
    SearchKind.Method, SearchKind.Transfer, SearchKind.Account
)

/** Kinds chosen in a sheet of the new UI, the others still open the dialogs of the classic one */
val SearchKind.hasSheet get() = this == SearchKind.Period || this == SearchKind.Amount || this == SearchKind.Status || this in listKinds

/**
 * Chooses the value of the filter of [kind]
 *
 * @param onApply with the new filter, null to remove it
 */
@Composable
fun SearchKindSheet(
    kind: SearchKind,
    part: SearchPart?,
    account: PageAccount,
    onApply: (SearchPart?) -> Unit,
    onDismiss: () -> Unit,
) {
    when (kind) {
        SearchKind.Period -> PeriodSheet(part, onApply, onDismiss)
        SearchKind.Amount -> AmountSheet(part, account, onApply, onDismiss)
        SearchKind.Status -> StatusSheet(part, onApply, onDismiss)
        in listKinds -> OptionsSheet(kind, part, account, onApply, onDismiss)
        else -> error("$kind has no sheet")
    }
}

/**
 * The frame of each sheet: title, the choice of [content], whether the filter is excluded, and the
 * buttons. [criterion] is what is chosen, null while the choice is incomplete.
 */
@Composable
private fun KindSheet(
    kind: SearchKind,
    part: SearchPart?,
    criterion: SimpleCriterion<*>?,
    onApply: (SearchPart?) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var excluded by rememberSaveable { mutableStateOf(part?.excluded == true) }
    FilterSheet(
        title = stringResource(kind.label),
        onDismiss = onDismiss,
        onApply = { criterion?.let { onApply(SearchPart(it, excluded)) } },
        applyLabel = stringResource(R.string.next_search_apply),
        resetLabel = stringResource(R.string.next_search_reset),
        onReset = part?.let { { onApply(null) } },
        applyEnabled = criterion != null,
    ) {
        content()
        SwitchRow(
            label = stringResource(R.string.next_search_exclude),
            checked = excluded,
            onCheckedChange = { excluded = it },
            supportingText = stringResource(R.string.next_search_exclude_hint),
            isFirst = true
        )
    }
}

@Composable
private fun PeriodSheet(part: SearchPart?, onApply: (SearchPart?) -> Unit, onDismiss: () -> Unit) {
    val today = remember { LocalDate.now() }
    val initial = part?.criterion as? DateCriterion
    val initialPreset = initial?.let { PeriodPreset.of(it, today) }
    var preset by rememberSaveable { mutableStateOf(initialPreset ?: PeriodPreset.ThisMonth.takeIf { initial == null }) }
    val bounds = initial?.takeIf { initialPreset == null }?.bounds()
    var start by rememberSaveable { mutableStateOf(bounds?.first) }
    var end by rememberSaveable { mutableStateOf(bounds?.second) }
    var editing by rememberSaveable { mutableStateOf<Boolean?>(null) } // true: start, false: end
    val dateFormatter = rememberDateFormatter(FormatStyle.MEDIUM)

    val criterion = preset?.criterion(today) ?: periodCriterion(start, end)
    KindSheet(SearchKind.Period, part, criterion, onApply, onDismiss) {
        PeriodPreset.entries.forEach { option ->
            val (from, until) = option.range(today)
            OptionRow(
                label = stringResource(option.label),
                selected = preset == option,
                onClick = { preset = option },
                isMultiple = false,
                supportingText = dateFormatter.formatRange(from, until)
            )
        }
        OptionRow(
            label = stringResource(R.string.next_search_custom_period),
            selected = preset == null,
            onClick = { preset = null },
            isMultiple = false
        )
        if (preset == null) {
            val open = stringResource(R.string.next_search_open)
            ValueRow(stringResource(R.string.next_search_from), start?.let(dateFormatter::format) ?: open, { editing = true }, isFirst = true)
            ValueRow(stringResource(R.string.next_search_until), end?.let(dateFormatter::format) ?: open, { editing = false })
        }
    }
    editing?.let { isStart ->
        DateDialog(
            initial = if (isStart) start else end,
            onConfirm = {
                if (isStart) start = it else end = it
                editing = null
            },
            onDismiss = { editing = null },
            onClear = {
                if (isStart) start = null else end = null
                editing = null
            },
            clearLabel = stringResource(R.string.next_search_open)
        )
    }
}

private val amountOperations = listOf(Operation.EQ, Operation.GTE, Operation.LTE, Operation.BTW)

@Composable
private fun AmountSheet(part: SearchPart?, account: PageAccount, onApply: (SearchPart?) -> Unit, onDismiss: () -> Unit) {
    val currency = account.currencyUnit
    val initial = part?.criterion as? AmountCriterion
    val initialUi = remember(initial) { initial?.transformForUi() }
    val toText = { minor: Long -> minor.toBigDecimal().movePointLeft(currency.fractionDigits).toPlainString() }
    var isIncome by rememberSaveable { mutableStateOf(initial?.sign == true) }
    var operation by rememberSaveable { mutableStateOf(initialUi?.first ?: Operation.GTE) }
    var first by rememberSaveable { mutableStateOf(initialUi?.second?.getOrNull(0)?.let(toText) ?: "") }
    var second by rememberSaveable { mutableStateOf(initialUi?.second?.getOrNull(1)?.let(toText) ?: "") }

    val value1 = parseAmount(first, currency.fractionDigits)
    val value2 = parseAmount(second, currency.fractionDigits)
    val criterion = value1?.takeIf { operation != Operation.BTW || value2 != null }?.let {
        AmountCriterion.create(operation, currency.code, isIncome, it, value2)
    }
    KindSheet(SearchKind.Amount, part, criterion, onApply, onDismiss) {
        val labels = mapOf(
            Operation.EQ to stringResource(R.string.next_search_exactly),
            Operation.GTE to stringResource(R.string.next_search_at_least),
            Operation.LTE to stringResource(R.string.next_search_at_most),
            Operation.BTW to stringResource(R.string.next_search_between),
        )
        val expense = stringResource(R.string.next_search_kind_expense)
        val income = stringResource(R.string.next_search_kind_income)
        val padding = Modifier.padding(horizontal = Spacing.screen, vertical = 4.dp)
        ChoiceChips(listOf(false, true), isIncome, { if (it) income else expense }, { isIncome = it }, padding)
        ChoiceChips(amountOperations, operation, { labels.getValue(it) }, { operation = it }, padding)
        Row(padding.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AmountField(first, { first = it }, currency.symbol, Modifier.weight(1f))
            if (operation == Operation.BTW) {
                Text(stringResource(R.string.next_search_amount_and), Modifier.padding(horizontal = 8.dp))
                AmountField(second, { second = it }, currency.symbol, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, symbol: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(stringResource(R.string.next_search_amount)) },
        singleLine = true,
        suffix = { Text(symbol) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun StatusSheet(part: SearchPart?, onApply: (SearchPart?) -> Unit, onDismiss: () -> Unit) {
    val initial = (part?.criterion as? CrStatusCriterion)?.values.orEmpty()
    var selected by rememberSaveable { mutableStateOf(initial.toSet()) }
    val criterion = selected.takeIf { it.isNotEmpty() }?.let { CrStatusCriterion(CrStatus.entries.filter { it in selected }) }
    KindSheet(SearchKind.Status, part, criterion, onApply, onDismiss) {
        CrStatus.entries.forEach { status ->
            OptionRow(
                label = stringResource(status.toStringRes()),
                selected = status in selected,
                onClick = { selected = if (status in selected) selected - status else selected + status },
                leading = { Dot(color = colorResource(status.color)) }
            )
        }
    }
}

/** Id of the option "None", e.g. transactions without payment method */
private const val NONE_ID = -1L

/** Lists longer than this get a field to find an option */
private const val SEARCHABLE_SIZE = 10

/**
 * Picks several options of a list loaded from the database, e.g. payment methods. Picking a
 * category also finds its subcategories.
 */
@Composable
private fun OptionsSheet(
    kind: SearchKind,
    part: SearchPart?,
    account: PageAccount,
    onApply: (SearchPart?) -> Unit,
    onDismiss: () -> Unit,
) {
    val contentResolver = LocalContext.current.contentResolver
    val options by produceState<List<SearchOption>?>(null, kind, account) {
        value = contentResolver.loadSearchOptions(kind, account)
    }
    val initial = (part?.criterion as? IdCriterion)?.values?.ifEmpty { listOf(NONE_ID) }.orEmpty()
    var selected by rememberSaveable { mutableStateOf(initial.toSet()) }
    val criterion = optionsCriterion(kind, selected, options.orEmpty())
    KindSheet(kind, part, criterion.takeIf { options != null }, onApply, onDismiss) {
        val loaded = options
        if (loaded == null) {
            LoadingRow()
        } else {
            OptionList(kind, loaded, selected, onSelectedChange = { selected = it })
        }
    }
}

@Composable
private fun OptionList(
    kind: SearchKind,
    options: List<SearchOption>,
    selected: Set<Long>,
    onSelectedChange: (Set<Long>) -> Unit,
) {
    // Transactions without tag, transfer or account cannot be searched
    val withNone = kind == SearchKind.Method || kind == SearchKind.Category || kind == SearchKind.Payee
    var query by rememberSaveable { mutableStateOf("") }
    if (options.size > SEARCHABLE_SIZE) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.next_search_find, stringResource(kind.label)),
            clearLabel = stringResource(R.string.next_search_clear),
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp)
        )
    }
    if (withNone && query.isBlank()) {
        OptionRow(
            label = stringResource(R.string.next_search_none),
            selected = NONE_ID in selected,
            // Transactions without method cannot be searched together with methods
            onClick = { onSelectedChange(if (NONE_ID in selected) emptySet() else setOf(NONE_ID)) }
        )
    }
    options.matching(query).forEach { option ->
        OptionRow(
            label = option.label,
            depth = option.depth,
            selected = option.id in selected,
            onClick = {
                onSelectedChange(if (option.id in selected) selected - option.id else selected - NONE_ID + option.id)
            }
        )
    }
}

/** The criterion for the picked options; null when nothing is picked */
private fun optionsCriterion(kind: SearchKind, ids: Set<Long>, options: List<SearchOption>): SimpleCriterion<*>? {
    if (ids.isEmpty()) return null
    val labels = options.associate { it.id to it.label }
    val label = ids.mapNotNull { labels[it] }.joinToString(", ")
    val none = NONE_ID in ids
    return when (kind) {
        SearchKind.Method -> if (none) MethodCriterion() else MethodCriterion(label, ids.toList())
        SearchKind.Category -> if (none) CategoryCriterion() else CategoryCriterion(label, ids.toList())
        SearchKind.Payee -> if (none) PayeeCriterion() else PayeeCriterion(label, ids.toList())
        SearchKind.Tag -> TagCriterion(label, ids.toList())
        SearchKind.Transfer -> TransferCriterion(label, ids.toList())
        else -> AccountCriterion(label, ids.toList())
    }
}

/** What the chip of a filter shows, e.g. "October 2026" for a period */
@Composable
fun SearchPart.label(): String {
    val context = LocalContext.current
    val value = when (val criterion = criterion) {
        is DateCriterion -> periodLabel(criterion)
        is IdCriterion -> if (criterion.values.isEmpty()) stringResource(R.string.next_search_none) else criterion.prettyPrint(context)
        else -> criterion.prettyPrint(context)
    }
    return if (excluded) stringResource(R.string.next_search_excluded, value) else value
}

@Composable
private fun periodLabel(criterion: DateCriterion): String {
    val today = remember { LocalDate.now() }
    val dateFormatter = rememberDateFormatter(FormatStyle.MEDIUM)
    val monthFormatter = rememberMonthFormatter()
    if (PeriodPreset.of(criterion, today) == PeriodPreset.Last30Days) return stringResource(R.string.next_search_last_30_days)
    return when (val shape = PeriodShape.of(criterion)) {
        is PeriodShape.Month -> monthFormatter.format(shape.first)
        is PeriodShape.Year -> shape.year.toString()
        null -> {
            val (start, end) = criterion.bounds()
            when {
                start != null && end != null -> dateFormatter.formatRange(start, end)
                start != null -> stringResource(R.string.next_search_from_date, dateFormatter.format(start))
                else -> stringResource(R.string.next_search_until_date, dateFormatter.format(end!!))
            }
        }
    }
}
