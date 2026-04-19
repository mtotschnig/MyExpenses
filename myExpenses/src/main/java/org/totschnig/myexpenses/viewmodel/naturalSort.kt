package org.totschnig.myexpenses.viewmodel

import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import java.util.Locale

fun getNaturalComparator(locale: Locale = Locale.getDefault()): Comparator<String> {

    val collator = Collator.getInstance(locale)

    if (collator is RuleBasedCollator) {
        collator.setNumericCollation(true)
    }

    collator.strength = Collator.TERTIARY

    return Comparator { s1, s2 -> collator.compare(s1, s2) }
}