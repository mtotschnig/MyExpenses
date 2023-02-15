package org.totschnig.myexpenses.activity

import android.content.Intent
import org.totschnig.myexpenses.util.enumValueOrDefault

enum class Action {
    SELECT_MAPPING, SELECT_FILTER, MANAGE
}

enum class HelpVariant {
    manage, select_mapping, select_filter
}

val Intent.asAction get() = enumValueOrDefault(action, Action.SELECT_MAPPING)