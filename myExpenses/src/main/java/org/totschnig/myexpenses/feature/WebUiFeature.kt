package org.totschnig.myexpenses.feature

import android.os.Binder
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber

const val STOP_ACTION = "STOP_ACTION"
const val START_ACTION = "START_ACTION"
const val RESTART_ACTION = "RESTART_ACTION"

abstract class WebUiBinder: Binder() {
    abstract fun getService(): IWebInputService?
}

interface IWebInputService {
    fun registerObserver(serverStateObserver: ServerStateObserver)
    fun unregisterObserver()

    companion object {
        private const val TAG = "WebInputService"
        fun log(message: String) {
            Timber.tag(TAG).w(message)
        }
        fun report(throwable: Throwable) {
            CrashHandler.report(throwable, TAG)
        }
    }
}

interface ServerStateObserver {
    fun postAddress(address: String)
    fun postException(throwable: Throwable)
    fun onStopped()
}