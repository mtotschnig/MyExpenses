package org.totschnig.webui

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.text.format.Formatter
import androidx.annotation.StringRes
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
import org.apache.commons.lang3.text.StrSubstitutor
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.IWebInputService
import org.totschnig.myexpenses.feature.START_ACTION
import org.totschnig.myexpenses.feature.STOP_ACTION
import org.totschnig.myexpenses.feature.ServerStateObserver
import org.totschnig.myexpenses.feature.WebUiBinder
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TPYE_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_WEB_UI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import javax.inject.Inject

private const val PORT = 9000
private const val STOP_CLICK_ACTION = "STOP_CLICK_ACTION"

class WebInputService : Service(), IWebInputService {

    @Inject
    lateinit var localDateJsonDeserializer: JsonDeserializer<LocalDate>

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    private lateinit var wrappedContext: Context

    private val binder = LocalBinder()

    private var serverStateObserver: ServerStateObserver? = null

    private var count = 0

    inner class LocalBinder : WebUiBinder() {
        override fun getService() = this@WebInputService
    }

    override fun onCreate() {
        super.onCreate()
        DaggerWebUiComponent.builder().appComponent((application as MyApplication).appComponent).build().inject(this)
        wrappedContext = ContextHelper.wrap(this, userLocaleProvider.getUserPreferredLocale())
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private var server: ApplicationEngine? = null

    private val serverAddress: String?
        get() = if (server != null) address else null

    override fun registerObserver(serverStateObserver: ServerStateObserver) {
        this.serverStateObserver = serverStateObserver
        serverAddress?.let { serverStateObserver.postAddress(it) }
    }

    override fun unregisterObserver() {
        this.serverStateObserver = null
    }

    private val address: String
        get() = "http://${(applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo.ipAddress.let { Formatter.formatIpAddress(it) }}:$PORT"


    private fun readFromAssets(fileName: String) = assets.open(fileName).bufferedReader()
            .use {
                it.readText()
            }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            STOP_CLICK_ACTION -> {
                prefHandler.putBoolean(PrefKey.UI_WEB, false)
            }
            STOP_ACTION -> {
                if (stopServer()) {
                    serverStateObserver?.onStopped()
                }
            }
            START_ACTION -> {
                if (server == null) {
                    server = embeddedServer(CIO, PORT, watchPaths = emptyList()) {
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
                                if (repository.createTransaction(call.receive()) != null) {
                                    count++
                                    call.respond(HttpStatusCode.Created, "${getString(R.string.save_transaction_and_new_success)} ($count)")
                                } else {
                                    call.respond(HttpStatusCode.Conflict, "Error while saving transaction.")
                                }
                            }
                            get("/styles.css") {
                                call.respondText(readFromAssets("styles.css"), ContentType.Text.CSS)
                            }
                            get("/") {
                                val data = mapOf(
                                        "accounts" to contentResolver.query(TransactionProvider.ACCOUNTS_BASE_URI,
                                                arrayOf(KEY_ROWID, KEY_LABEL, KEY_TYPE),
                                                DatabaseConstants.KEY_SEALED + " = 0", null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map {
                                                        mapOf(
                                                                "id" to it.getLong(0),
                                                                "label" to it.getString(1),
                                                                "type" to it.getString(2)
                                                        )
                                                    }
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
                                        },
                                        "methods" to contentResolver.query(TransactionProvider.METHODS_URI,
                                                arrayOf(KEY_ROWID, KEY_LABEL, KEY_IS_NUMBERED, KEY_TYPE, KEY_ACCOUNT_TPYE_LIST),
                                                null, null, null)?.use {
                                            generateSequence { if (it.moveToNext()) it else null }
                                                    .map {
                                                        mapOf(
                                                                "id" to it.getLong(0),
                                                                "label" to it.getString(1),
                                                                "isNumbered" to (it.getInt(2) > 0),
                                                                "type" to it.getInt(3),
                                                                "accountTypes" to it.getString(4)?.split(',')
                                                        )
                                                    }
                                                    .toList()
                                        },
                                )
                                val text = StrSubstitutor.replace(readFromAssets("form.html"), mapOf(
                                        "i18n_title" to "${t(R.string.app_name)} ${getString(R.string.title_webui)}",
                                        "i18n_account" to t(R.string.account),
                                        "i18n_amount" to t(R.string.amount),
                                        "i18n_date" to t(R.string.date),
                                        "i18n_payee" to t(R.string.payer_or_payee),
                                        "i18n_category" to t(R.string.category),
                                        "i18n_tags" to t(R.string.tags),
                                        "i18n_notes" to t(R.string.comment),
                                        "i18n_method" to t(R.string.method),
                                        "i18n_submit" to t(R.string.menu_save),
                                        "i18n_number" to t(R.string.reference_number),
                                        "data" to gson.toJson(data)))
                                call.respondText(text, ContentType.Text.Html)
                            }
                        }
                    }.also {
                        it.start(wait = false)
                    }

                    val stopIntent = Intent(this, WebInputService::class.java).apply {
                        action = STOP_CLICK_ACTION
                    }
                    val notification: Notification = NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, getString(R.string.title_webui), address)
                            .addAction(0, 0, getString(R.string.stop), PendingIntent.getService(this, 0, stopIntent, FLAG_ONE_SHOT))
                            .build()

                    startForeground(NOTIFICATION_WEB_UI, notification)
                    serverStateObserver?.postAddress(address)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun t(@StringRes resId: Int) = wrappedContext.getString(resId)

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    private fun stopServer() = if (server != null) {
        server?.stop(0, 0)
        server = null
        stopForeground(true)
        true
    } else false
}