package org.totschnig.myexpenses.next

import android.app.Activity
import android.content.Intent
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.next.settings.NextSettings

/**
 * Central place where the new UI decides which screen a command opens.
 *
 * Commands are the menu ids dispatched by the main screen (e.g. `R.id.MANAGE_TEMPLATES_COMMAND`).
 * Return `true` if the command was handled by a screen of the new UI; return `false` to fall
 * back to the old screen (handled by `MyExpensesV2` / `BaseMyExpenses`).
 *
 * To replace an old screen: build the new screen in this module, then add its command here, e.g.
 *
 * ```
 * R.id.MANAGE_TEMPLATES_COMMAND -> {
 *     activity.startActivity(Intent(activity, NextTemplates::class.java))
 *     true
 * }
 * ```
 */
object NextRouter {
    @Suppress("UNUSED_PARAMETER")
    fun route(activity: Activity, command: Int, tag: Any?): Boolean = when (command) {
        R.id.SETTINGS_COMMAND -> {
            // For the result of a restore, see BaseActivity.withResultCallbackLauncher
            (activity as BaseActivity).withResultCallbackLauncher.launch(Intent(activity, NextSettings::class.java))
            true
        }
        else -> false
    }
}
