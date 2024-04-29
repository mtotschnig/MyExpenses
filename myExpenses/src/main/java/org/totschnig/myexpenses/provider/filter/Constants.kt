package org.totschnig.myexpenses.provider.filter

import android.app.Activity
import android.os.Bundle
import androidx.core.os.BundleCompat

const val NULL_ITEM_ID = -1L
const val KEY_SELECTION = "selection"
const val KEY_CRITERION = "criterion"

val Activity.preSelected: List<Long>?
    get() = intent.getLongArrayExtra(KEY_SELECTION)?.toList()

fun <T> Bundle.criterion(clazz: Class<T>): T? = BundleCompat.getParcelable(this, KEY_CRITERION, clazz)
