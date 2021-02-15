package org.totschnig.myexpenses.feature

import android.os.Binder

const val STOP_ACTION = "STOP_ACTION"
const val START_ACTION = "START_ACTION"

abstract class WebUiBinder: Binder() {
    abstract fun getService(): IWebInputService
}

interface IWebInputService {
    fun registerObserver(serverStateObserver: ServerStateObserver)
    fun unregisterObserver()
}

fun interface ServerStateObserver {
    /**
     * @param address: The address to reach the server or null if the server is stopped
     */
    fun onChanged(address: String?)
}