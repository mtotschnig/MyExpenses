package org.totschnig.myexpenses.service

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.text.format.Formatter
import androidx.lifecycle.LifecycleService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_WEB_UI
import timber.log.Timber

const val STOP_ACTION = "STOP_ACTION"
const val START_ACTION = "START_ACTION"

class WebInputService : LifecycleService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO  + job)

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebInputService = this@WebInputService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private var server: ApplicationEngine? = null
    val isServerRunning
        get() = server != null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            STOP_ACTION -> {
                stopServer()
            }
            START_ACTION -> {
                if (server == null) {
                    //setting watchPaths to null prevents crash on server stop on Lollipop
                    // (https://youtrack.jetbrains.com/issue/KTOR-1613)  and prevents Strict Mode violation
                    server = embeddedServer(Netty, 9000, watchPaths = emptyList()) {
                        install(ContentNegotiation) {
                            gson {}
                        }
                        routing {
                            post("/") {
                                Timber.d("Received %s", call.receiveText())
                                call.response.header("Access-Control-Allow-Origin", "*")
                                call.respond(mapOf("message" to "Hello world"))
                            }
                            get("/") {
                                call.response.header("Access-Control-Allow-Origin", "*")
                                call.respond(mapOf(
                                        "accounts" to arrayOf(
                                                mapOf("id" to 1, "label" to "Bankkonto"),
                                                mapOf("id" to 2, "label" to "Geldtasche")),
                                        "payees" to arrayOf(
                                                mapOf("id" to 1, "name" to "A A"),
                                                mapOf("id" to 2, "name" to "B B"))
                                ))
                            }
                        }
                    }.also {
                        //TODO strict mode. start on background
                        scope.launch {
                            it.start(wait = false)
                        }
                    }

                    val stopIntent = Intent(this, WebInputService::class.java).apply {
                        action = STOP_ACTION
                    }
                    val notification: Notification = NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, "Web UI", "Running ..." +
                            ((applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager)?.connectionInfo?.ipAddress?.let { Formatter.formatIpAddress(it) }
                                    ?: "?"))
                            .addAction(0, 0, "Stop", PendingIntent.getService(this, 0, stopIntent, FLAG_ONE_SHOT))
                            .build()

                    startForeground(NOTIFICATION_WEB_UI, notification)

                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
        job.cancel()
    }

    private fun stopServer() {
        //TODO strict mode. stop on background
        server?.stop(0, 0)
        server = null
        stopForeground(true)
    }
}