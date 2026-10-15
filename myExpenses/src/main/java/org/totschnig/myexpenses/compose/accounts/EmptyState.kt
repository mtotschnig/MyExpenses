package org.totschnig.myexpenses.compose.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R

@Composable
fun EmptyState(
    onCreateAccount: () -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(dimensionResource(id = R.dimen.padding_main_screen)),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.wrapContentSize(),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.no_accounts)
        )
        Button(onClick = onCreateAccount) {
            Text(text = stringResource(id = R.string.menu_create_account))
        }
        onNavigateToSettings?.let {
            Button(onClick = it) {
                Text(text = stringResource(R.string.settings_label))
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400)
@Composable
fun EmptyStatePreview() {
    Box(contentAlignment = Alignment.Center) {
        EmptyState()
    }
}