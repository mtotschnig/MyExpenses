package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.feature.IWebInputService
import org.totschnig.myexpenses.feature.ServerStateObserver
import org.totschnig.myexpenses.feature.WebUiBinder

class WebUiViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var webInputService: IWebInputService
    private var webInputServiceBound: Boolean = false
    private val serviceState: MutableLiveData<String?> = MutableLiveData()
    fun getServiceState(): LiveData<String?> = serviceState
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            webInputService = (service as WebUiBinder).getService()
            webInputServiceBound = true
            webInputService.registerObserver(object: ServerStateObserver {
                override fun postAddress(address: String) {
                    serviceState.postValue(address)
                }

                override fun onStopped() {
                    serviceState.postValue(null)
                }

            })
        }

        override fun onServiceDisconnected(className: ComponentName) {
            webInputServiceBound = false
        }
    }

    fun bind(context: Context) {
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    }

    fun unbind(context: Context) {
        if (webInputServiceBound) {
            context.unbindService(serviceConnection)
            webInputServiceBound = false
        }
    }

    companion object {
        val serviceIntent: Intent
            get() = Intent().apply {
                setClassName(BuildConfig.APPLICATION_ID, "org.totschnig.webui.WebInputService")
            }
    }
}