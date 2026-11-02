package org.totschnig.myexpenses.next

import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import java.io.Serializable
import org.totschnig.myexpenses.activity.MyExpensesV2
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.designsystem.ActionChipRow
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.next.search.SearchResult
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.transactions.toShown
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount

/**
 * Entry point of the new UI, started from its own launcher icon ("MyExpenses Next").
 *
 * Reuses all of [MyExpensesV2] (view model, dialogs, event handling) and only replaces
 * what is exposed through its hooks:
 * - [MainTheme]: look and feel ([NextTheme])
 * - [MainScreen]: the main screen ([NextMainScreen])
 * - [dispatchCommand]: navigation to other screens ([NextRouter])
 */
class MyExpensesNext : MyExpensesV2() {

    // The areas of the new UI, see [NextFeature]
    private val features by lazy { NextFeatures(this) }

    @Composable
    override fun MainTheme(content: @Composable () -> Unit) {
        NextTheme(content = content)
    }

    @Composable
    override fun MainScreen(
        viewModel: MyExpensesV2ViewModel,
        accounts: List<FullAccount>,
        allCurrencies: List<CurrencyUnit>,
        availableFilters: List<AccountGroupingKey>,
        selectedAccountId: Long,
        onAppEvent: AppEventHandler,
        onAccountEvent: AccountEventHandler,
        onPrepareContextMenuItem: (itemId: Int) -> Boolean,
        onPrepareMenuItem: (itemId: Int) -> Boolean,
        flags: List<AccountFlag>,
        bankIcon: (@Composable (Modifier, Long) -> Unit)?,
        adView: @Composable (MutableState<Boolean>) -> Unit,
        isNavigationVisible: Boolean,
        isCurrencyUsed: suspend (String) -> Boolean,
        onCreateAsset: suspend (code: String, symbol: String, fractionDigits: Int, label: String?, commodityType: CommodityType) -> CurrencyUnit?,
        pageContent: @Composable (pageAccount: PageAccount, isCurrent: Boolean) -> Unit,
    ) {
        // What the new UI adds to the details of a transaction, in the accounts and in lists of other screens
        val transactionDetails: @Composable (ShownTransaction) -> Unit = { transaction ->
            features.all.forEach { it.TransactionSections(transaction) }
            ActionChipRow { features.all.forEach { it.TransactionChips(transaction) } }
        }
        val rows = features.all.mapNotNull { it.rowDecorations() }
        CompositionLocalProvider(*features.all.flatMap { it.provided() }.toTypedArray()) {
            NextMainScreen(
                viewModel = viewModel,
                accounts = accounts,
                allCurrencies = allCurrencies,
                selectedAccountId = selectedAccountId,
                onAppEvent = onAppEvent,
                onAccountEvent = onAccountEvent,
                onPrepareContextMenuItem = onPrepareContextMenuItem,
                onPrepareMenuItem = onPrepareMenuItem,
                bankIcon = bankIcon,
                adView = adView,
                isNavigationVisible = isNavigationVisible,
                isCurrencyUsed = isCurrencyUsed,
                onCreateAsset = onCreateAsset,
                pageContent = pageContent,
                transactionList = { pageAccount, search ->
                    // Day groups with the balance at the end of each day only make sense when sorted by date
                    val grouping = if (pageAccount.sortBy == KEY_DATE) Grouping.DAY else Grouping.NONE
                    Page(pageAccount.copy(grouping = grouping), accounts.size, true, v2 = true, withFilterUi = false, transactionList = { content ->
                        if (content.isFiltered) (content.headerData as? HeaderData)?.let { headerData ->
                            val items = content.lazyPagingItems
                            SearchResult(
                                headerData,
                                count = items.itemCount.takeIf { items.loadState.append.endOfPaginationReached },
                                onReset = search::reset
                            )
                        }
                        NextTransactionList(
                            content,
                            Modifier.weight(1f),
                            detailsContent = { transactionDetails(it.toShown()) },
                            suggestedCategory = rows.firstNotNullOfOrNull { it.suggestedCategory } ?: { null },
                            categoryChip = rows.firstNotNullOfOrNull { it.categoryChip } ?: { null },
                            tagChips = rows.firstNotNullOfOrNull { it.tagChips } ?: { null },
                            isMarker = rows.firstNotNullOfOrNull { it.isMarker } ?: { false },
                            payeeImage = rows.firstNotNullOfOrNull { it.payeeImage } ?: { null },
                            payeeImageDialog = rows.firstNotNullOfOrNull { it.payeeImageDialog }
                        )
                    })
                },
                transactionDetails = transactionDetails,
                features = features.all
            )
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (features.all.none { it.onContribFeatureCalled(feature, tag) }) super.contribFeatureCalled(feature, tag)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean =
        NextRouter.route(this, command, tag) || super.dispatchCommand(command, tag)
}

/**
 * A view model of the activity created by [create], e.g. with the facts it reads. [create] gets the
 * saved state of the view model, for what has to survive when Android ends the process.
 */
internal inline fun <reified VM : ViewModel> ComponentActivity.viewModelsOf(noinline create: (SavedStateHandle) -> VM) =
    viewModels<VM> { viewModelFactory { initializer { create(createSavedStateHandle()) } } }
