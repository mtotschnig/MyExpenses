package org.totschnig.myexpenses.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.text.format.Formatter
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import timber.log.Timber

class WebInputService : Service() {
    private lateinit var server: ApplicationEngine
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server = embeddedServer(Netty, 9000) {
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
                    call.respond(mapOf("accounts" to arrayOf(mapOf("id" to 1, "label" to "Bankkonto"), mapOf("id" to 2, "label" to "Geldtasche"))))
                }
            }
        }
        server.start(wait = false)
        val notification: Notification = NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, "Web UI", "Running ..." +
                ((applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager)?.connectionInfo?.ipAddress?.let { Formatter.formatIpAddress(it) }
                        ?: "?"))
                .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        server.stop(1000, 1000)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}