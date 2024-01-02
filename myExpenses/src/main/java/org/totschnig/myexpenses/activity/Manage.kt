package org.totschnig.myexpenses.activity

import android.content.Intent
import org.totschnig.myexpenses.util.enumValueOrDefault

enum class Action {
    SELECT_MAPPING, SELECT_FILTER, MANAGE
}

const val HELP_VARIANT_MANGE = "manage"
const val HELP_VARIANT_SELECT_MAPPING = "select_mapping"
const val HELP_VARIANT_SELECT_FILTER = "select_filter"

val Intent.asAction get() = enumValueOrDefault(action, Action.SELECT_MAPPING)