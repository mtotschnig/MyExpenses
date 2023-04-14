package org.totschnig.myexpenses.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.filter.WhereFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterCard(whereFilter: WhereFilter?, clearFilter: () -> Unit) {
    Row(
        modifier = Modifier
            .background(color = colorResource(id = R.color.cardBackground))
            .padding(horizontal = dimensionResource(R.dimen.padding_main_screen)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.menu_search),
            tint = Color.Green
        )
        FlowRow(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            whereFilter?.criteria?.forEach {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = {
                        Text(it.prettyPrint(LocalContext.current))
                    }
                )
            }
        }
        IconButton(onClick = clearFilter) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.clear_all_filters)
            )
        }
    }
}