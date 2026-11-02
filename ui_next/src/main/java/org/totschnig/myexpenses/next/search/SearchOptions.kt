package org.totschnig.myexpenses.next.search

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelection
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionArgs
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionArgsV2
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionV2
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.util.Utils
import java.text.Collator
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.viewmodel.data.PageAccount

/**
 * Something a filter can be set to, e.g. a payment method
 *
 * @param parentId the option above this one, e.g. the main category of a subcategory
 * @param depth how far below the top this option is, 0 for a main category
 */
data class SearchOption(val id: Long, val label: String, val parentId: Long? = null, val depth: Int = 0)

/**
 * The options whose label contains [query], ignoring case and accents, together with the options
 * above them, so a subcategory is shown below its category
 */
fun List<SearchOption>.matching(query: String): List<SearchOption> {
    val normalized = Utils.normalize(query.trim()).takeIf { it.isNotEmpty() } ?: return this
    val byId = associateBy { it.id }
    val kept = HashSet<Long>()
    filter { Utils.normalize(it.label).contains(normalized) }.forEach { match ->
        var option: SearchOption? = match
        while (option != null && kept.add(option.id)) option = option.parentId?.let(byId::get)
    }
    return filter { it.id in kept }
}

/** Sorts [options] by label below their parent, depth first */
internal fun tree(options: List<SearchOption>): List<SearchOption> {
    val collator = Collator.getInstance()
    val children = options.groupBy { it.parentId }
    val ids = options.mapTo(HashSet()) { it.id }
    return buildList {
        fun add(option: SearchOption, depth: Int) {
            add(option.copy(depth = depth))
            children[option.id].orEmpty().sortedWith { a, b -> collator.compare(a.label, b.label) }
                .forEach { add(it, depth + 1) }
        }
        // Options whose parent is missing are shown at the top, too
        options.filter { it.parentId == null || it.parentId !in ids }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .forEach { add(it, 0) }
    }
}

/**
 * The options of [kind] for [account], like the classic filter dialogs offer them: the payment
 * methods and transfer accounts used by its transactions, the accounts of the same currency for an
 * aggregate, and all categories, payees and tags. Categories come as a tree. Empty for other kinds.
 */
suspend fun ContentResolver.loadSearchOptions(kind: SearchKind, account: PageAccount): List<SearchOption> =
    withContext(Dispatchers.IO) {
        when (kind) {
            SearchKind.Method -> query(TransactionProvider.MAPPED_METHODS_URI, account.mappedSelection())
            SearchKind.Transfer -> query(TransactionProvider.MAPPED_TRANSFER_ACCOUNTS_URI, account.mappedSelection())
            SearchKind.Category -> tree(
                query(
                    TransactionProvider.CATEGORIES_URI, "$KEY_ROWID != $SPLIT_CATID" to null,
                    withParent = true
                )
            )

            // Payees below another one are found with it
            SearchKind.Payee -> tree(
                query(TransactionProvider.PAYEES_URI, "$KEY_PARENTID IS NULL" to null, labelColumn = KEY_PAYEE_NAME)
            )

            SearchKind.Tag -> tree(query(TransactionProvider.TAGS_URI, null to null))

            SearchKind.Account -> {
                val currency = account.currency.takeIf { !account.isHomeAggregate }
                val selection = if (currency == null) "$KEY_PARENTID IS NULL"
                else "$KEY_PARENTID IS NULL AND $KEY_CURRENCY = ?"
                query(TransactionProvider.ACCOUNTS_BASE_URI, selection to currency?.let { arrayOf(it) })
            }

            else -> emptyList()
        }
    }

private fun ContentResolver.query(
    uri: Uri,
    selection: Pair<String?, Array<String>?>,
    labelColumn: String = KEY_LABEL,
    withParent: Boolean = false,
) = query(
    uri,
    listOfNotNull(KEY_ROWID, labelColumn, KEY_PARENTID.takeIf { withParent }).toTypedArray(),
    selection.first, selection.second, null
)?.use { cursor ->
    buildList {
        while (cursor.moveToNext()) add(
            SearchOption(
                cursor.getLong(KEY_ROWID),
                cursor.getString(labelColumn),
                if (withParent) cursor.getLongOrNull(KEY_PARENTID) else null
            )
        )
    }
} ?: emptyList()

/** The transactions of the account, as [SelectFromMappedTableDialogFragment] selects them */
private fun PageAccount.mappedSelection(): Pair<String?, Array<String>?> {
    val grouping = accountGrouping
    return if (id == 0L && grouping != null) {
        val group = when (grouping) {
            AccountGrouping.CURRENCY -> currency
            AccountGrouping.FLAG -> flag.id.toString()
            AccountGrouping.TYPE -> type.id.toString()
            else -> "Unit"
        }
        accountSelectionV2(grouping) to accountSelectionArgsV2(grouping, group)
    } else accountSelection(id).takeIf { it.isNotEmpty() } to accountSelectionArgs(id)
}
