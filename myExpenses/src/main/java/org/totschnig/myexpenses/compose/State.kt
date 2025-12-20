package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap

/**
 * A helper function to create a simple, immutable State object for previews
 * or for providing a default value to a Composable that requires a State parameter.
 */
@Composable
fun <T> rememberStaticState(value: T): State<T> {
    return remember {
        object : State<T> {
            override val value: T = value
        }
    }
}


//https://stackoverflow.com/a/68887484/1199911
@Composable
fun <T> rememberMutableStateListOf(elements: List<T> = emptyList()) = rememberSaveable(
    saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
) {
    elements.toMutableStateList()
}

@Composable
fun <K, V> rememberMutableStateMapOf(vararg pairs: Pair<K, V>) =
    rememberSaveable(
        saver = mapSaver(
            save = { snapshotStateMap ->
                // Save the map by "flattening" it into a list of keys and values
                // with unique string identifiers. This is the most robust pattern.
                val savedMap = mutableMapOf<String, Any?>()
                snapshotStateMap.entries.forEachIndexed { index, entry ->
                    savedMap["k$index"] = entry.key
                    savedMap["v$index"] = entry.value
                }
                savedMap
            },
            restore = { savedMap ->
                // Restore the map by iterating through the saved keys.
                val restoredPairs = (0 until savedMap.size / 2).map { index ->
                    // Re-create the key-value pairs from the flattened map.
                    val key = savedMap["k$index"] as K
                    val value = savedMap["v$index"] as V
                    key to value
                }
                restoredPairs.toMutableStateMap()
            }
        )
    ) {
        // The initial value provided to rememberSaveable
        pairs.toList().toMutableStateMap()
    }

@Composable
fun <K, V> rememberMutableStateMapOf(defaultValue: V, vararg inputs: Any?) =
    rememberSaveable(
        inputs = inputs,
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateMap().withDefault { defaultValue } }
        )
    ) {
        mutableStateMapOf<K, V>().withDefault { defaultValue }
    }


fun <T> MutableState<List<T>>.toggle(element: T) = if (value.contains(element)) {
    value = value - element
    false
} else {
    value = value + element
    true
}

fun <T> MutableState<List<T>>.addToSelection(element: T) {
    if (!value.contains(element)) {
        value = value + element
    }
}

fun <T> MutableState<List<T>>.select(element: T) {
    value = listOf(element)
}

fun <T> MutableState<List<T>>.unselect(selector: (T) -> Boolean) {
    value = value.filterNot { selector(it) }
}
