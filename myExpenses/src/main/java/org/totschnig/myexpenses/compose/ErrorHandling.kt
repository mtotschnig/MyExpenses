package org.totschnig.myexpenses.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R

@Composable
fun PagingErrorItem(
    message: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun PagingErrorPage(
    message: String,
    modifier: Modifier = Modifier,
    onSafeModeClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_main_screen)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        if (onSafeModeClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start safe mode",
                modifier = Modifier
                    .clickable(onClick = onSafeModeClick)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
