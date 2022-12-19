package org.totschnig.myexpenses.feature

import android.os.Binder

const val STOP_ACTION = "STOP_ACTION"
const val START_ACTION = "START_ACTION"
const val RESTART_ACTION = "RESTART_ACTION"

abstract class WebUiBinder: Binder() {
    abstract fun getService(): IWebInputService?
}

interface IWebInputService {
    fun registerObserver(serverStateObserver: ServerStateObserver)
    fun unregisterObserver()
}

interface ServerStateObserver {
    fun postAddress(address: String)
    fun postException(throwable: Throwable)
    fun onStopped()
}