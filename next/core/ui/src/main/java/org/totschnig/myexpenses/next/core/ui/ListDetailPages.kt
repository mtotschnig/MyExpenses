package org.totschnig.myexpenses.next.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.scene.SceneStrategy
import org.totschnig.myexpenses.designsystem.EmptyState

/**
 * The scene strategy of the back stack of a feature that shows its list and the page opened from
 * it side by side in wide windows, e.g. the contracts and one contract. Its entries say where they
 * go with [ListPage], [DetailPage] and [ExtraPage]; in narrow windows they are shown one at a time.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : Any> rememberListDetailPages(): SceneStrategy<T> = rememberListDetailSceneStrategy()

/**
 * Metadata of the entry of the list
 *
 * @param placeholder shown beside the list while nothing is opened, e.g. "Choose a contract"
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun ListPage(@StringRes placeholder: Int): Map<String, Any> = ListDetailSceneStrategy.listPane(
    detailPlaceholder = {
        EmptyState(stringResource(placeholder), Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
    }
)

/** Metadata of a page opened from the list, shown beside it */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun DetailPage(): Map<String, Any> = ListDetailSceneStrategy.detailPane()

/** Metadata of a page opened from a [DetailPage], e.g. its transactions, shown beside it instead of the list */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun ExtraPage(): Map<String, Any> = ListDetailSceneStrategy.extraPane()
