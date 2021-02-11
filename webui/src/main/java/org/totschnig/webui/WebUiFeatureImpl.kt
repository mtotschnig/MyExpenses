package org.totschnig.webui

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.Keep
import org.totschnig.myexpenses.feature.WebUiFeature

@Keep
class WebUiFeatureImpl : WebUiFeature {
    private lateinit var webInputService: WebInputService
    private var webInputServiceBound: Boolean = false
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            webInputService = (service as WebInputService.LocalBinder).getService()
            webInputServiceBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            webInputServiceBound = false
        }
    }

    override fun bind(context: Context) {
        Intent(context, WebInputService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun unbind(context: Context) {
        if (webInputServiceBound) {
            context.unbindService(serviceConnection)
        }
        webInputServiceBound = false
    }

    override fun toggle(context: Context) {
        context.startService(Intent(context, WebInputService::class.java).apply {
            action = if (isBoundAndRunning) STOP_ACTION else START_ACTION
        })
    }

    override val isBoundAndRunning: Boolean
        get() = webInputServiceBound && webInputService.isServerRunning


}