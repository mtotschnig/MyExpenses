package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.totschnig.myexpenses.next.core.ui.CategoryIconTile
import org.totschnig.myexpenses.next.core.ui.merchants.TransactionTile
import org.totschnig.myexpenses.next.core.ui.icons.CatalogIconTile
import org.totschnig.myexpenses.next.core.common.icons.IconCatalog
import org.totschnig.myexpenses.next.core.ui.merchants.LocalPayeeImages
import org.totschnig.myexpenses.next.data.contracts.BuiltInArea
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.icon

/**
 * The tile of [contract]: the icon the user chose, else what is shown for its payee, else an icon
 * that fits its name or its area, else the icon of its category
 *
 * @param isLarge in the header of the details, small in lists
 */
@Composable
fun ContractTile(contract: Contract, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    val chosen = contract.rule?.icon
    if (chosen != null) CatalogIconTile(chosen, modifier, isLarge)
    else AutomaticContractTile(contract, modifier, isLarge)
}

/** [ContractTile] without the icon the user chose */
@Composable
fun AutomaticContractTile(contract: Contract, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    val payeeImage = contract.lastTransaction.payeeId?.let(LocalPayeeImages.current)
    val icon = contract.automaticIcon
    when {
        payeeImage != null -> TransactionTile(contract.title, payeeImage, contract.categoryIcon, modifier, isLarge)
        icon != null -> CatalogIconTile(icon, modifier, isLarge)
        else -> CategoryIconTile(contract.categoryIcon, modifier, isLarge)
    }
}

/** An icon that fits the name of the contract, else one of its area, null if neither has one */
val Contract.automaticIcon: String?
    get() = IconCatalog.bestFor(displayName) ?: (area as? BuiltInArea)?.icon

/** Icons to choose from for [contract]: from its name, area and category */
val Contract.iconSuggestions: List<String>
    get() = listOfNotNull(categoryIcon, (area as? BuiltInArea)?.icon) +
            IconCatalog.suggest(displayName, name, *categoryPath?.split('>')?.asReversed()?.toTypedArray().orEmpty())
