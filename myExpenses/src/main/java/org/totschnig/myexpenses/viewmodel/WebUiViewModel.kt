package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.feature.IWebInputService
import org.totschnig.myexpenses.feature.ServerStateObserver
import org.totschnig.myexpenses.feature.WebUiBinder
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class WebUiViewModel(application: Application) : AndroidViewModel(application) {
    private var webInputService: IWebInputService? = null
    private var webInputServiceBound: Boolean = false
    private val _serviceState: MutableSharedFlow<Result<String?>> = MutableSharedFlow(extraBufferCapacity = 1)
    val serviceState: SharedFlow<Result<String?>> = _serviceState
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            webInputService = (service as WebUiBinder).getService()?.apply {
                registerObserver(object: ServerStateObserver {
                    override fun postAddress(address: String) {
                        _serviceState.tryEmit(Result.success(address))
                    }

                    override fun postException(throwable: Throwable) {
                        _serviceState.tryEmit(Result.failure(throwable))
                    }

                    override fun onStopped() {
                        _serviceState.tryEmit(Result.success(null))
                    }
                })
            }
            webInputServiceBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            webInputServiceBound = false
        }
    }

    fun bind(context: Context) {
        serviceIntent?.let { context.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE) }
    }

    fun unbind(context: Context) {
        if (webInputServiceBound) {
            context.unbindService(serviceConnection)
            webInputService?.unregisterObserver()
            webInputService = null
            webInputServiceBound = false
        }
    }

    companion object {
        val serviceIntent: Intent?
            get() = try {
                Class.forName("org.totschnig.webui.WebInputService")
            } catch (e: ClassNotFoundException) {
                CrashHandler.report(e)
                null
            }?.let {
                Intent().apply {
                    setClassName(BuildConfig.APPLICATION_ID, it.name)
                }
            }


    }
}