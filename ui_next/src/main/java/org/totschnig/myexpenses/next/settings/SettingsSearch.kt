package org.totschnig.myexpenses.next.settings

import java.text.Normalizer

/**
 * The entries that contain every word of [query]: first those found by their title or where they
 * are, i.e. group and section, then those found only by their [details], e.g. a summary or the
 * options of a list. Both in the order of the catalog. Case and accents do not matter, so "uber"
 * finds "Über".
 */
fun searchSettings(
    query: String,
    entries: List<SettingEntry>,
    title: (SettingEntry) -> String,
    place: (SettingEntry) -> String,
    details: (SettingEntry) -> String,
): List<SettingEntry> {
    val words = query.normalized().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    fun String.hasAll() = normalized().let { text -> words.all { it in text } }
    val (byName, others) = entries.partition { "${title(it)} ${place(it)}".hasAll() }
    return byName + others.filter { "${title(it)} ${place(it)} ${details(it)}".hasAll() }
}

private val diacritics = Regex("\\p{Mn}+")

private fun String.normalized() =
    diacritics.replace(Normalizer.normalize(lowercase(), Normalizer.Form.NFD), "").trim()
