package org.totschnig.myexpenses.compose.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R

private val ftsHelpItems = listOf(
    R.string.search_fts_help_and,
    R.string.search_fts_help_not,
    R.string.search_fts_help_or,
    R.string.search_fts_help_phrase,
    R.string.search_fts_help_prefix,
    R.string.search_fts_help_column
)

@Composable
fun FtsHelpList(modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ftsHelpItems.forEach { resId ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("•")
                    Text(
                        text = stringResource(resId),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
