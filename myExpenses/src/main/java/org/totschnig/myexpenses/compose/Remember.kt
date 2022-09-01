package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

//https://stackoverflow.com/a/68887484/1199911
@Composable
fun <T> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> {
    return rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        elements.toList().toMutableStateList()
    }
}

// Currently not needed, but might be in the future
/*@Composable
fun <K, V> rememberMutableStateMapOf(vararg pairs: Pair<K, V>) : SnapshotStateMap<K, V> {
    return rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateMap() }
        )
    ) {
        pairs.toList().toMutableStateMap()
    }
}*/

/**
 * true if element was added, false if it was removed
 */
fun <T> MutableList<T>.toggle(element: T) = if (contains(element)) {
    remove(element)
    false
} else {
    add(element)
    true
}