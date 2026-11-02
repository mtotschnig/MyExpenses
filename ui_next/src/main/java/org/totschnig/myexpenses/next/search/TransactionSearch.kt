package org.totschnig.myexpenses.next.search

import org.totschnig.myexpenses.next.toInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.compose.filter.FilterHandler
import org.totschnig.myexpenses.designsystem.ConfirmDialog
import org.totschnig.myexpenses.designsystem.SearchFilterChip
import org.totschnig.myexpenses.designsystem.SearchFilterRow
import org.totschnig.myexpenses.designsystem.SearchResultRow
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberAmountFormat
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.PageAccount

/**
 * The search of the transactions of an account. Its filter is the one stored for the account, so
 * the classic UI and the lists that use the filter, e.g. the export, see the same.
 *
 * The search is active while it was opened or a filter is stored. Typing is stored a moment after
 * the last key, so the list does not reload for each letter.
 */
@Stable
class TransactionSearch internal constructor(
    private val stored: State<Criterion?>,
    private val opened: MutableState<Boolean>,
    private val typed: MutableState<String?>,
    private val persistence: FilterPersistence,
    private val scope: CoroutineScope,
) {
    val filter: SearchFilter get() = SearchFilter.of(stored.value)

    val isActive get() = opened.value || stored.value != null

    /** What the search field shows: what was typed, else the text stored */
    val query: String get() = typed.value ?: filter.text?.searchString ?: ""

    fun open() {
        opened.value = true
    }

    fun onQueryChange(query: String) {
        typed.value = query
    }

    /** Changes the stored filter, based on its latest value */
    fun update(transform: (SearchFilter) -> SearchFilter) {
        scope.launch { persistence.update { transform(SearchFilter.of(it)).toCriterion() } }
    }

    /** Removes all filters, the search stays open */
    fun reset() {
        typed.value = ""
        scope.launch { persistence.persist(null) }
    }

    /** Removes all filters and closes the search */
    fun close() {
        reset()
        typed.value = null
        opened.value = false
    }
}

/**
 * @param fractionDigits of the currency of the account, to find a typed number as amount
 * @param scope stores the filter, of the view model, so that a rotation does not cancel it
 */
@Composable
fun rememberTransactionSearch(
    accountId: Long,
    persistence: FilterPersistence,
    fractionDigits: Int,
    scope: CoroutineScope,
): TransactionSearch {
    val stored = persistence.whereFilter.collectAsState()
    val opened = rememberSaveable(accountId) { mutableStateOf(false) }
    val typed = rememberSaveable(accountId) { mutableStateOf<String?>(null) }
    LaunchedEffect(typed.value) {
        val query = typed.value ?: return@LaunchedEffect
        delay(300)
        persistence.update { SearchFilter.of(it).withText(searchText(query, fractionDigits)).toCriterion() }
    }
    return remember(accountId, persistence, scope) { TransactionSearch(stored, opened, typed, persistence, scope) }
}

/**
 * The kinds of filter that make sense for [account]: e.g. categories only if transactions have one
 */
private fun availableKinds(account: PageAccount, sumInfo: SumInfo) = buildList {
    add(SearchKind.Period)
    if (sumInfo.mappedCategories) add(SearchKind.Category)
    if (sumInfo.mappedPayees) add(SearchKind.Payee)
    add(SearchKind.Amount)
    if (sumInfo.hasTags) add(SearchKind.Tag)
    if (account.isAggregate || account.type.supportsReconciliation) add(SearchKind.Status)
    if (sumInfo.mappedMethods) add(SearchKind.Method)
    if (sumInfo.hasTransfers) add(SearchKind.Transfer)
    if (account.isAggregate) add(SearchKind.Account)
}

/**
 * The filters of [search] as chips below the search field. A chip opens the choice of its value.
 * An advanced filter from the classic UI is a single chip that can only be removed.
 */
@Composable
fun SearchFilterChips(search: TransactionSearch, account: PageAccount, sumInfo: SumInfo) {
    val context = LocalContext.current
    // Leave the search field, otherwise the keyboard comes back when a sheet or dialog closes
    val focusManager = LocalFocusManager.current
    when (val filter = search.filter) {
        is SearchFilter.Advanced -> {
            var confirmRemove by rememberSaveable { mutableStateOf(false) }
            SearchFilterRow {
                SearchFilterChip(
                    label = stringResource(R.string.next_search_advanced),
                    value = stringResource(R.string.next_search_advanced),
                    onClick = {
                        focusManager.clearFocus()
                        confirmRemove = true
                    }
                )
            }
            if (confirmRemove) {
                ConfirmDialog(
                    title = stringResource(R.string.next_search_advanced_title),
                    text = stringResource(R.string.next_search_advanced_text, filter.criterion.prettyPrint(context)),
                    confirmLabel = stringResource(R.string.next_search_advanced_remove),
                    onConfirm = {
                        confirmRemove = false
                        search.update { SearchFilter.Simple().withText(it.text) }
                    },
                    onDismiss = { confirmRemove = false }
                )
            }
        }

        is SearchFilter.Simple -> {
            // Kinds that are set stay visible, even if they do not apply to the account any more
            val kinds = (availableKinds(account, sumInfo) + filter.parts.keys - SearchKind.Text)
                .distinct().sorted()
            var editing by rememberSaveable { mutableStateOf<SearchKind?>(null) }
            val apply = { kind: SearchKind, part: SearchPart? ->
                search.update { current -> (current as? SearchFilter.Simple)?.with(kind, part) ?: current }
            }
            FilterHandler(account, "nextSearch_${account.id}", { _, newValue ->
                val kind = newValue?.let { SearchKind.of(it) } ?: return@FilterHandler
                search.update { current ->
                    (current as? SearchFilter.Simple)
                        ?.with(kind, SearchPart(newValue, excluded = current[kind]?.excluded == true))
                        ?: current
                }
            }) {
                SearchFilterRow {
                    kinds.forEach { kind ->
                        val part = filter[kind]
                        SearchFilterChip(
                            label = stringResource(kind.label),
                            value = part?.label(),
                            onClick = {
                                focusManager.clearFocus()
                                when {
                                    kind.hasSheet -> editing = kind
                                    part != null -> handleEdit(part.criterion)
                                    else -> handleEdit(kind.clazz)
                                }
                            }
                        )
                    }
                }
            }
            editing?.let { kind ->
                SearchKindSheet(
                    kind = kind,
                    part = filter[kind],
                    account = account,
                    onApply = {
                        apply(kind, it)
                        editing = null
                    },
                    onDismiss = { editing = null }
                )
            }
        }
    }
}

/**
 * What the search found, from the sums of the days of [headerData]
 *
 * @param count of the transactions found, null while not all are loaded
 */
@Composable
fun SearchResult(headerData: HeaderData, count: Int?, onReset: () -> Unit) {
    val format = rememberAmountFormat(headerData.account.currencyUnit.toInfo())
    val expenses = headerData.groups.values.sumOf { it.expenseSum.amountMinor }
    val income = headerData.groups.values.sumOf { it.incomeSum.amountMinor }
    val text = listOfNotNull(
        count?.let { pluralStringResource(R.plurals.next_search_count, it, it) },
        expenses.takeIf { it != 0L }?.let { stringResource(R.string.next_search_expenses, format(it)) },
        income.takeIf { it != 0L }?.let { stringResource(R.string.next_search_income, format(it)) },
    ).joinToString(" · ")
    SearchResultRow(text, actionLabel = stringResource(R.string.next_search_reset), onAction = onReset)
}
