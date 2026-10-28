package org.totschnig.myexpenses.next.feature.contracts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import java.time.LocalDate
import org.totschnig.myexpenses.next.core.ui.DetailPage
import org.totschnig.myexpenses.next.core.ui.ExtraPage
import org.totschnig.myexpenses.next.core.ui.ListPage
import org.totschnig.myexpenses.next.core.ui.rememberListDetailPages
import org.totschnig.myexpenses.designsystem.NameDialog
import org.totschnig.myexpenses.designsystem.PageTopBar
import org.totschnig.myexpenses.designsystem.PromptCard
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.shownOrLeave
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.valueOrNull
import org.totschnig.myexpenses.next.data.contracts.ContractArea
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.ContractSettings
import org.totschnig.myexpenses.next.data.contracts.ContractTransaction
import org.totschnig.myexpenses.next.data.contracts.SalaryChoice
import org.totschnig.myexpenses.next.data.contracts.buildContractsState
import org.totschnig.myexpenses.next.data.contracts.contractTransactions
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.next.data.contracts.salaries
import org.totschnig.myexpenses.next.data.contracts.ContractsOverview
import org.totschnig.myexpenses.next.data.contracts.Contract

/** The screens of the contracts, the list at the bottom of the back stack */
@Serializable
sealed interface ContractsKey : NavKey {
    /** The contracts with the tabs of their categories, or the incomes */
    @Serializable
    data object Home : ContractsKey

    /**
     * @param payment a payment of the contract: confirming a suggestion gives the contract the key
     * of its new rule, it is then found by the payment
     */
    @Serializable
    data class Detail(val signature: String, val payment: Long?) : ContractsKey

    /** The debits of the contract of [Detail] */
    @Serializable
    data class Transactions(val signature: String, val payment: Long?) : ContractsKey

    @Serializable
    data object NewsHistory : ContractsKey

    /** @param areaKey category of the tab the suggestions were opened from, null for all contracts */
    @Serializable
    data class Suggestions(val areaKey: String?) : ContractsKey
}

private fun Contract.toDetail() = ContractsKey.Detail(signature, transactions.first().id)

private fun ContractsOverview.find(signature: String, payment: Long?): Contract? {
    val all = active + ended + suggestions + cancelled
    return all.find { it.signature == signature }
        ?: payment?.let { id -> all.find { contract -> contract.transactions.any { it.id == id } } }
}

/**
 * Contracts detected from recurring debits, grouped by how often they are debited.
 *
 * Like the account list of the overview, the list has an edit mode. In it, the user removes
 * contracts that were detected by mistake, restores them and renames contracts.
 * Besides all contracts, tabs show the contracts of each category ([ContractArea]) that has some,
 * and each category created by the user. The last tab creates a new category.
 *
 * The details, debits, news and suggestions are screens on a back stack of their own ([ContractsKey]),
 * so that back returns to where they were opened from.
 *
 * @param currency the currency all amounts of the contracts are in
 */
@Composable
fun NextContractsScreen(
    state: AnalysisState<ContractsOverview>,
    currency: CurrencyInfo,
    modifier: Modifier = Modifier,
    /** What the user can do with contracts, see [ContractActions] */
    actions: ContractActions = object : ContractActions {},
    onConsent: (Boolean) -> Unit = {},
    /** Renders the debits of a contract, see [ContractTransactionList] */
    contractTransactions: @Composable (Contract, Modifier) -> Unit = { _, _ -> },
    /**
     * Shows regular incomes instead of contracts: without categories, with a title and [onBack]
     * instead of the tabs
     */
    isIncome: Boolean = false,
    onBack: () -> Unit = {},
    /** News about contracts and incomes, the ones of this list are shown */
    news: List<ContractNews> = emptyList(),
    /** Signature of a contract shown on top of the list from the start */
    openContract: String? = null,
) {
    val backStack = rememberNavBackStack(
        *listOfNotNull<NavKey>(ContractsKey.Home, openContract?.let { ContractsKey.Detail(it, null) }).toTypedArray()
    )
    // The entries read the latest values, whenever NavDisplay creates their content
    val current by rememberUpdatedState(
        ContractsContent(state, currency, actions, onConsent, contractTransactions, isIncome, onBack, news)
    )
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        sceneStrategies = listOf(rememberListDetailPages()),
        entryProvider = entryProvider {
            entry<ContractsKey.Home>(metadata = ListPage(if (isIncome) R.string.next_pick_income else R.string.next_pick_contract)) { ContractsHome(current, backStack) }
            entry<ContractsKey.Detail>(metadata = DetailPage()) { key -> ContractDetailEntry(current, backStack, key) }
            entry<ContractsKey.Transactions>(metadata = ExtraPage()) { key -> ContractTransactionsEntry(current, backStack, key) }
            entry<ContractsKey.NewsHistory>(metadata = DetailPage()) { NewsHistoryEntry(current, backStack) }
            entry<ContractsKey.Suggestions>(metadata = DetailPage()) { key -> SuggestionsEntry(current, backStack, key) }
        }
    )
}

/** The parameters of [NextContractsScreen] as the screens on its back stack need them */
private class ContractsContent(
    val state: AnalysisState<ContractsOverview>,
    val currency: CurrencyInfo,
    val actions: ContractActions,
    val onConsent: (Boolean) -> Unit,
    val contractTransactions: @Composable (Contract, Modifier) -> Unit,
    val isIncome: Boolean,
    val onBack: () -> Unit,
    news: List<ContractNews>,
) {
    val ready = state.valueOrNull

    /** The news of this list */
    val listNews = news.filter { it.isIncome == isIncome }
    val unreadNews = listNews.filter { !it.isRead }
}

/** The contract of the screen, the screen is left once it is gone, e.g. declared as no contract */
@Composable
private fun shownContract(content: ContractsContent, backStack: NavBackStack<NavKey>, key: ContractsKey, signature: String, payment: Long?) =
    shownOrLeave(content.ready?.find(signature, payment), isLoaded = content.ready != null) { backStack.remove(key) }

@Composable
private fun ContractDetailEntry(content: ContractsContent, backStack: NavBackStack<NavKey>, key: ContractsKey.Detail) {
    val contract = shownContract(content, backStack, key, key.signature, key.payment) ?: return
    val ready = content.ready ?: return
    var showSalarySheet by rememberSaveable { mutableStateOf(false) }
    if (showSalarySheet) {
        SalarySheet(
            incomes = ready.active,
            choice = ready.salaryChoice,
            onSelect = {
                content.actions.setSalary(it)
                showSalarySheet = false
            },
            onDismiss = { showSalarySheet = false }
        )
    }
    val isSalary = ready.isSalary(contract)
    // Opening the contract reads its news, they stay visible until it is closed
    val openedNews = remember(contract.signature) {
        content.unreadNews.filter { it.contractKey == contract.signature }
    }
    LaunchedEffect(contract.signature) {
        if (openedNews.isNotEmpty()) content.actions.markNewsRead(openedNews.mapTo(HashSet()) { it.id })
    }
    ContractDetailScreen(
        contract = contract,
        currency = content.currency,
        news = openedNews,
        history = content.listNews.filter { it.contractKey == contract.signature },
        onBack = { backStack.remove(key) },
        actions = content.actions,
        mergeCandidates = (ready.active + ready.ended + ready.suggestions + ready.cancelled)
            .filter { it.signature != contract.signature },
        areas = ready.selectableAreas,
        onShowTransactions = { backStack.add(ContractsKey.Transactions(key.signature, key.payment)) },
        isSalary = isSalary,
        // Adding an income to the salaries is one tap, changing them needs the choice
        onSalaryClick = {
            if (isSalary) showSalarySheet = true
            else content.actions.setSalary(
                SalaryChoice.Fixed(ready.salaries.map { it.signature }.toSet() + contract.signature)
            )
        }
    )
}

@Composable
private fun ContractTransactionsEntry(content: ContractsContent, backStack: NavBackStack<NavKey>, key: ContractsKey.Transactions) {
    val contract = shownContract(content, backStack, key, key.signature, key.payment) ?: return
    ContractTransactionsScreen(contract = contract, onBack = { backStack.remove(key) }) {
        content.contractTransactions(contract, it)
    }
}

@Composable
private fun NewsHistoryEntry(content: ContractsContent, backStack: NavBackStack<NavKey>) {
    NewsHistoryScreen(
        news = content.listNews,
        currency = content.currency,
        onBack = { backStack.remove(ContractsKey.NewsHistory) },
        onOpen = { item ->
            val contract = content.ready?.let { (it.active + it.ended + it.cancelled).find { contract -> contract.signature == item.contractKey } }
            if (contract != null) {
                backStack.add(contract.toDetail())
            } else {
                // The contract is gone, e.g. declared as no contract
                content.actions.markNewsRead(setOf(item.id))
            }
        },
        onMarkAllRead = { content.actions.markNewsRead(content.unreadNews.mapTo(HashSet()) { it.id }) }
    )
}

@Composable
private fun SuggestionsEntry(content: ContractsContent, backStack: NavBackStack<NavKey>, key: ContractsKey.Suggestions) {
    val ready = content.ready ?: return
    val area = ready.areas.find { it.key == key.areaKey }
    ContractSuggestionsScreen(
        suggestions = (area?.let(ready::forArea) ?: ready).suggestions,
        isIncome = content.isIncome,
        currency = content.currency,
        actions = content.actions,
        onOpen = { backStack.add(it.toDetail()) },
        onBack = { backStack.remove(key) }
    )
}

@Composable
private fun ContractsHome(content: ContractsContent, backStack: NavBackStack<NavKey>) {
    val state = content.state
    val ready = content.ready
    val actions = content.actions
    /** Key of the category of the selected tab, null for all contracts */
    var selectedAreaKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateAreaDialog by rememberSaveable { mutableStateOf(false) }
    val areas = if (content.isIncome) emptyList() else ready?.areas.orEmpty()
    // The category may have lost its last contract or been deleted
    val selectedArea = areas.find { it.key == selectedAreaKey }
    Column(Modifier.fillMaxSize()) {
        if (showCreateAreaDialog) {
            NameDialog(
                title = stringResource(R.string.next_contracts_new_area),
                initialName = "",
                onConfirm = {
                    selectedAreaKey = actions.createArea(it).key
                    showCreateAreaDialog = false
                },
                onDismiss = { showCreateAreaDialog = false }
            )
        }
        if (content.isIncome) {
            PageTopBar(onBack = content.onBack, title = stringResource(R.string.next_income_title))
        } else AreaTabs(
            areas = areas,
            selected = selectedArea,
            onSelect = { selectedAreaKey = it?.key },
            onCreate = { showCreateAreaDialog = true }.takeIf { ready != null }
        )
        val contentModifier = Modifier.weight(1f)
        when (state) {
            AnalysisState.Loading -> CircularProgressIndicator(
                contentModifier
                    .fillMaxSize()
                    .wrapContentSize()
            )

            AnalysisState.AskConsent -> ConsentCard(
                isDeclined = false,
                onConsent = content.onConsent,
                modifier = contentModifier
            )

            AnalysisState.Declined -> ConsentCard(
                isDeclined = true,
                onConsent = content.onConsent,
                modifier = contentModifier
            )

            is AnalysisState.Ready -> ContractList(
                state = selectedArea?.let(state.value::forArea) ?: state.value,
                area = selectedArea,
                isIncome = content.isIncome,
                currency = content.currency,
                actions = actions,
                onOpen = { backStack.add(it.toDetail()) },
                news = content.unreadNews,
                onOpenNews = { backStack += ContractsKey.NewsHistory }.takeIf { content.listNews.isNotEmpty() },
                onOpenSuggestions = { backStack.add(ContractsKey.Suggestions(selectedArea?.key)) },
                modifier = contentModifier
            )
        }
    }
}


/**
 * Asks whether transactions may be analysed. Once declined, only offers to start the analysis.
 */
/**
 * Tabs for all contracts and each category, and one to create a category
 *
 * @param selected null for all contracts
 * @param onCreate null while the categories are not known yet
 */
@Composable
private fun AreaTabs(
    areas: List<ContractArea>,
    selected: ContractArea?,
    onSelect: (ContractArea?) -> Unit,
    onCreate: (() -> Unit)?,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selected?.let { areas.indexOf(it) + 1 } ?: 0,
        edgePadding = 16.dp
    ) {
        Tab(
            selected = selected == null,
            onClick = { onSelect(null) },
            text = { Text(stringResource(R.string.next_contracts_tab_all)) }
        )
        areas.forEach { area ->
            Tab(
                selected = area == selected,
                onClick = { onSelect(area) },
                text = { Text(area.label()) }
            )
        }
        if (onCreate != null) {
            Tab(
                selected = false,
                onClick = onCreate,
                icon = {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.next_contracts_new_area))
                }
            )
        }
    }
}

@Composable
private fun ConsentCard(
    isDeclined: Boolean,
    onConsent: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PromptCard(
        icon = Icons.Default.Search,
        title = stringResource(R.string.next_contracts_consent_title),
        text = stringResource(
            if (isDeclined) R.string.next_contracts_declined
            else R.string.next_contracts_consent_text
        ),
        confirmLabel = stringResource(R.string.next_contracts_consent_accept),
        onConfirm = { onConsent(true) },
        dismissLabel = if (isDeclined) null else stringResource(R.string.next_contracts_consent_decline),
        onDismiss = { onConsent(false) },
        modifier = modifier
    )
}

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    NextPreviewTheme {
        Surface(content = content)
    }
}

private fun previewState(): ContractsOverview {
    val today = LocalDate.now()
    fun contract(
        name: String,
        interval: ContractInterval,
        vararg amounts: Long,
        category: String? = null,
        last: LocalDate = today.minusDays(5),
        isConfirmed: Boolean = true,
    ) =
        Contract(
            signature = name,
            name = name,
            interval = interval,
            transactions = amounts.mapIndexed { i, amount ->
                ContractTransaction(
                    id = i.toLong(),
                    date = last.minus(interval.step.multipliedBy(amounts.size - 1 - i)),
                    amount = -amount,
                    accountId = 1,
                    accountLabel = "Girokonto",
                    categoryPath = category
                )
            },
            nextExpectedDate = last.plus(interval.step),
            isActive = last.isAfter(today.minusDays(interval.maxDays.toLong())),
            isConfirmed = isConfirmed
        )
    return buildContractsState(
        listOf(
            contract("Netflix", ContractInterval.MONTHLY, 1299, 1299, 1799, category = "Freizeit > Streaming"),
            contract("Stadtwerke", ContractInterval.MONTHLY, 8500, 8500, 8500, category = "Wohnen > Strom"),
            contract("Kfz-Versicherung", ContractInterval.QUARTERLY, 12050, 12050, 12050, category = "Auto > Kfz-Versicherung"),
            contract("ADAC", ContractInterval.YEARLY, 9400, 9400, isConfirmed = false),
            contract(
                "Fitnessstudio", ContractInterval.MONTHLY, 2990, 2990, 2990,
                category = "Freizeit > Fitness", last = today.minusMonths(5)
            ),
            contract("Bäckerei", ContractInterval.WEEKLY, 450, 480, 450, 470),
        ),
        ContractSettings(dismissed = setOf("Bäckerei"), names = mapOf("Stadtwerke" to "Strom"))
    )
}

@Preview(name = "Light", showBackground = true, heightDp = 800)
@Preview(name = "Dark", showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NextContractsScreenPreview() {
    PreviewTheme {
        NextContractsScreen(
            state = AnalysisState.Ready(previewState()),
            currency = CurrencyInfo.Preview,
            news = listOf(
                ContractNews(
                    id = "preview", type = ContractNews.Type.PRICE_CHANGE, ruleId = "netflix", transactionId = 1,
                    date = LocalDate.now().minusDays(5), createdAt = LocalDate.now(), contractName = "Netflix",
                    isIncome = false, amount = 1799, previousAmount = 1299
                )
            )
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun ConsentPreview() {
    PreviewTheme {
        NextContractsScreen(state = AnalysisState.AskConsent, currency = CurrencyInfo.Preview)
    }
}
