package org.totschnig.webui

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.text.format.Formatter
import androidx.lifecycle.LifecycleService
import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.commons.lang3.text.StrSubstitutor
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_WEB_UI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import javax.inject.Inject

const val STOP_ACTION = "STOP_ACTION"
const val START_ACTION = "START_ACTION"

class WebInputService : LifecycleService() {

    @Inject
    lateinit var localDateJsonDeserializer: JsonDeserializer<LocalDate>

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var gson: Gson

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebInputService = this@WebInputService
    }

    override fun onCreate() {
        super.onCreate()
        DaggerWebUiComponent.builder().appComponent((application as MyApplication).appComponent).build().inject(this)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private var server: ApplicationEngine? = null
    val isServerRunning
        get() = server != null

    private fun readFromAssets(fileName: String) = assets.open(fileName).bufferedReader()
            .use {
                it.readText()
            }

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
                    server = embeddedServer(CIO, 9000, watchPaths = emptyList()) {
                        install(ContentNegotiation) {
                            gson {
                                registerTypeAdapter(LocalDate::class.java, localDateJsonDeserializer)
                            }
                        }
                        install(StatusPages) {
                            exception<Throwable> { cause ->
                                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                                CrashHandler.report(cause)
                                throw cause
                            }
                        }
                        routing {
                            post("/") {
                                call.respond(if (repository.createTransaction(call.receive()) != null) HttpStatusCode.Created else HttpStatusCode.Conflict)
                            }
                            get("/styles.css") {
                                call.respondText(readFromAssets("styles.css"), ContentType.Text.CSS)
                            }
                            get("/") {
                                val data = mapOf(
                                        "accounts" to contentResolver.query(TransactionProvider.ACCOUNTS_BASE_URI,
                                                arrayOf(KEY_ROWID, KEY_LABEL),
                                                DatabaseConstants.KEY_SEALED + " = 0", null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map { mapOf("id" to it.getLong(0), "label" to it.getString(1)) }
                                                    .toList()
                                        },
                                        "payees" to contentResolver.query(TransactionProvider.PAYEES_URI,
                                                arrayOf(KEY_ROWID, KEY_PAYEE_NAME),
                                                null, null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map { mapOf("id" to it.getLong(0), "name" to it.getString(1)) }
                                                    .toList()
                                        },
                                        "categories" to contentResolver.query(TransactionProvider.CATEGORIES_URI,
                                                arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL),
                                                null, null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map { mapOf("id" to it.getLong(0), "parent" to it.getLong(1), "label" to it.getString(2)) }
                                                    .toList()
                                        },
                                        "tags" to contentResolver.query(TransactionProvider.TAGS_URI,
                                                arrayOf(KEY_ROWID, KEY_LABEL),
                                                null, null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map { mapOf("id" to it.getLong(0), "label" to it.getString(1)) }
                                                    .toList()
                                        }
                                )
                                val text = StrSubstitutor.replace(readFromAssets("form.html"), mapOf(
                                        "i18n_title" to "${getString(R.string.app_name)} ${getString(R.string.title_webui)}",
                                        "i18n_account" to getString(R.string.account),
                                        "i18n_amount" to getString(R.string.amount),
                                        "i18n_date" to getString(R.string.date),
                                        "i18n_payee" to getString(R.string.payer_or_payee),
                                        "i18n_category" to getString(R.string.category),
                                        "i18n_tags" to getString(R.string.tags),
                                        "i18n_notes" to getString(R.string.comment),
                                        "i18n_submit" to getString(R.string.menu_save),
                                        "data" to gson.toJson(data)))
                                call.respondText(text, ContentType.Text.Html)
                            }
                        }
                    }.also {
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