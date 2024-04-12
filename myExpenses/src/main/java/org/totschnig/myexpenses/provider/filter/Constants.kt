package org.totschnig.myexpenses.provider.filter

import android.app.Activity

const val NULL_ITEM_ID = -1L
const val KEY_SELECTION = "selection"

val Activity.preSelected: List<Long>?
    get() = intent.getLongArrayExtra(KEY_SELECTION)?.toList()
