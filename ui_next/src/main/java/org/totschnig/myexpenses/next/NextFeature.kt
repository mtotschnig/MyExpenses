package org.totschnig.myexpenses.next

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.model.ContribFeature
import java.io.Serializable
import org.totschnig.myexpenses.viewmodel.data.Transaction2

/**
 * What one area adds to the shell, e.g. its sections in the details of a transaction, its cards on
 * the overview or its tab. Each area has one in its package of the shell, which builds the view
 * models of the area; [NextFeatures] lists them. So an area can be added or left out with its
 * modules, its entry and one line in [NextFeatures], without changing the screens of the shell.
 *
 * All parts are empty by default; the shell shows those of all features in the order of
 * [NextFeatures.all].
 */
interface NextFeature {
    /** Sections in the details of a transaction, in the accounts and in lists of other screens */
    @Composable
    fun TransactionSections(transaction: ShownTransaction) {}

    /** Chips in the row of actions below the details of a transaction */
    @Composable
    fun TransactionChips(transaction: ShownTransaction) {}

    /** Questions and hints at the top of the overview, e.g. whether to categorize automatically */
    @Composable
    fun OverviewBanner(navigation: NextNavigation) {}

    /** Cards of the overview below the banners, e.g. the balance of the month */
    @Composable
    fun OverviewCard(navigation: NextNavigation) {}

    /** The budgets of the overview, only from the feature that has them */
    @Composable
    fun overviewBudgets(navigation: NextNavigation): OverviewBudgets? = null

    /** Floating button of the overview, e.g. to scan a receipt */
    @Composable
    fun OverviewAction() {}

    /** What the area shows on the rows of the transaction list, e.g. a chip with a suggested category */
    @Composable
    fun rowDecorations(): RowDecorations? = null

    /** Values the area provides to the screens below the main screen, e.g. the logos of payees */
    @Composable
    fun provided(): List<ProvidedValue<*>> = emptyList()

    /** The tab of the area in the bar at the bottom or the rail at the start, if it has one */
    val tab: NextTab? get() = null

    /** Whether the area has something new, shown as a dot on its [tab] */
    @Composable
    fun hasNews(): Boolean = false

    /** The pages of the area on the back stack of the new UI, one entry per [NextKey] of the area */
    fun EntryProviderScope<NavKey>.pages(shell: NextShell) {}

    /**
     * The user may now use [feature] of the licence, which the area asked for with [tag], see
     * `contribFeatureRequested`. True if it was the request of the area.
     */
    fun onContribFeatureCalled(feature: ContribFeature, tag: Serializable?): Boolean = false
}

/** What features may do with the pages of the new UI */
interface NextNavigation {
    /** Shows the page of [key] on top, so that back leads to where it was opened */
    fun show(key: NextKey)

    /** Closes the page of [key] */
    fun close(key: NextKey)
}

/** What the shell gives the pages of the areas */
class NextShell(
    val navigation: NextNavigation,
    /** What all areas add to the details of a transaction */
    val transactionDetails: @Composable (ShownTransaction) -> Unit,
)

/**
 * What an area shows on the rows of the transaction list; the shell takes each part from the first
 * feature that has it, see [NextTransactionList]
 */
class RowDecorations(
    /** Path of the category suggested for a transaction without one */
    val suggestedCategory: ((transactionId: Long) -> String?)? = null,
    val categoryChip: ((Transaction2) -> (@Composable (Modifier) -> Unit)?)? = null,
    val tagChips: ((Transaction2) -> (@Composable (Modifier) -> Unit)?)? = null,
    /** Whether a tag is a marker (one-off, reimbursed, business) rather than a group */
    val isMarker: ((tagId: Long) -> Boolean)? = null,
    val payeeImage: ((payeeId: Long) -> PayeeImage?)? = null,
    val payeeImageDialog: (@Composable (payee: DisplayParty, transaction: Transaction2?, onDismiss: () -> Unit) -> Unit)? = null,
)
