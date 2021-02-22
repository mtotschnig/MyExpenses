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

interface ServerStateObserver {
    fun postAddress(address: String)
    fun onStopped()
}