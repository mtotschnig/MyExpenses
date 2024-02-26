package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap

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
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateMap() }
        )
    ) {
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
