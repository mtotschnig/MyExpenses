package org.totschnig.myexpenses.compose.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.provider.filter.FtsCriterion

private val ftsHelpItems = listOf(
    R.string.search_fts_help_and,
    R.string.search_fts_help_not,
    R.string.search_fts_help_or,
    R.string.search_fts_help_phrase,
    R.string.search_fts_help_prefix,
    R.string.search_fts_help_column
)

@Composable
private fun FtsHelpList(modifier: Modifier = Modifier) {
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

@Composable
fun FtsInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val isMalformed = FtsCriterion.isMalformedFtsQuery(value)

    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .conditional(focusRequester != null) {
                    focusRequester(focusRequester!!)
                },
            value = value,
            onValueChange = onValueChange,
            isError = isMalformed,
            label = { Text(stringResource(R.string.search_full_text)) },
            supportingText = if (isMalformed) {
                {
                    Text(
                        text = stringResource(R.string.search_fts_error_malformed),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showHelp = !showHelp }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.menu_help)
                        )
                    }
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.search_clear_desc)
                            )
                        }
                    }
                }
            }
        )
        AnimatedVisibility(visible = showHelp) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                FtsHelpList()
            }
        }
    }
}
