package org.totschnig.myexpenses.next.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * What a page on a back stack shows, looked up in the current state of its feature, e.g. a contract
 * by its signature. Gone from the loaded state, e.g. deleted, [onGone] leaves the page. While the
 * state is loading, a progress indicator is shown.
 *
 * @param awaitNew the value was just created, e.g. a pot just saved, and is waited for until it
 * was shown once
 * @return [value], null while the page shows nothing else
 */
@Composable
fun <T : Any> shownOrLeave(value: T?, isLoaded: Boolean, awaitNew: Boolean = false, onGone: () -> Unit): T? {
    var wasShown by rememberSaveable { mutableStateOf(false) }
    if (value != null && !wasShown) wasShown = true
    if (value == null) {
        if (isLoaded && (wasShown || !awaitNew)) LaunchedEffect(Unit) { onGone() }
        else CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
    }
    return value
}
