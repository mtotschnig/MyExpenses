package org.totschnig.myexpenses.util

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

fun BroadcastReceiver.doAsync(
    block: suspend () -> Unit
) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            try {
                pendingResult.finish()
            } catch (e: Exception) {
                CrashHandler.report(e)
            }
        }
    }
}