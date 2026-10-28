package org.totschnig.myexpenses.next.insights

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The day of the device, for [org.totschnig.myexpenses.next.core.model.ledger.LedgerSource.today].
 * Emits at midnight and when the user sets the date, the time or the time zone, which a timer
 * alone would miss, since it counts time since the boot.
 */
fun today(context: Context): Flow<LocalDate> = channelFlow {
    val changes = Channel<Unit>(Channel.CONFLATED)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            changes.trySend(Unit)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    val appContext = context.applicationContext
    ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    try {
        while (true) {
            val now = LocalDateTime.now()
            send(now.toLocalDate())
            val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1000
            withTimeoutOrNull(untilMidnight) { changes.receive() }
        }
    } finally {
        appContext.unregisterReceiver(receiver)
    }
}
