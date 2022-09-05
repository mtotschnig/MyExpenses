package org.totschnig.webui

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.database.getLongOrNull
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.StringSubstitutor.*
import org.apache.commons.text.lookup.StringLookup
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.di.LocalDateAdapter
import org.totschnig.myexpenses.di.LocalTimeAdapter
import org.totschnig.myexpenses.feature.IWebInputService
import org.totschnig.myexpenses.feature.START_ACTION
import org.totschnig.myexpenses.feature.STOP_ACTION
import org.totschnig.myexpenses.feature.ServerStateObserver
import org.totschnig.myexpenses.feature.WebUiBinder
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_WEB_UI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.getWifiIpAddress
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.IOException
import java.net.ServerSocket
import java.security.Security
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

private const val STOP_CLICK_ACTION = "STOP_CLICK_ACTION"

class WebInputService : LifecycleService(), IWebInputService {

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var currencyContext: CurrencyContext

    private lateinit var wrappedContext: Context

    private val binder = LocalBinder()

    private var serverStateObserver: ServerStateObserver? = null

    private var port: Int = 0

    private var useHttps: Boolean = false

    inner class LocalBinder : WebUiBinder() {
        override fun getService() = this@WebInputService
    }

    override fun onCreate() {
        super.onCreate()
        DaggerWebUiComponent.builder().appComponent((application as MyApplication).appComponent)
            .build().inject(this)
        wrappedContext = ContextHelper.wrap(this, userLocaleProvider.getUserPreferredLocale())
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
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

    private val protocol: String
        get() = if (useHttps) "https" else "http"

    private val address: String
        get() = "$protocol://${getWifiIpAddress(this)}:$port"


    private fun readTextFromAssets(fileName: String) = assets.open(fileName).bufferedReader()
        .use {
            it.readText()
        }

    private fun readBytesFromAssets(fileName: String) = assets.open(fileName).use {
        it.readBytes()
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
                if (server != null) {
                    stopServer()
                }
                useHttps = prefHandler.getBoolean(PrefKey.WEBUI_HTTPS, false)
                if (try {
                        (9000..9050).first { isAvailable(it) }
                    } catch (e: NoSuchElementException) {
                        serverStateObserver?.postException(IOException("No available port found in range 9000..9050"))
                        0
                    }.let { port = it; it != 0 }
                ) {
                    val environment = applicationEngineEnvironment {
                        if (useHttps) {
                            val keystore = generateCertificate(keyAlias = "myKey")
                            sslConnector(
                                keyStore = keystore,
                                keyAlias = "myKey",
                                keyStorePassword = { charArrayOf() },
                                privateKeyPassword = { charArrayOf() }) {
                                port = this@WebInputService.port
                            }
                        } else {
                            connector {
                                port = this@WebInputService.port
                            }
                        }
                        watchPaths = emptyList()
                        module {
                            install(ContentNegotiation) {
                                gson {
                                    registerTypeAdapter(
                                        LocalDate::class.java,
                                        LocalDateAdapter
                                    )
                                    registerTypeAdapter(
                                        LocalTime::class.java,
                                        LocalTimeAdapter
                                    )
                                }
                            }
                            val passWord = prefHandler.getString(PrefKey.WEBUI_PASSWORD, "")
                                .takeIf { !it.isNullOrBlank() }
                            passWord?.let {
                                install(Authentication) {
                                    basic("auth-basic") {
                                        realm = getString(R.string.app_name)
                                        validate { credentials ->
                                            if (credentials.password == it) {
                                                UserIdPrincipal(credentials.name)
                                            } else {
                                                null
                                            }
                                        }
                                    }
                                }
                            }

                            install(StatusPages) {
                                exception<Throwable> { call, cause ->
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        "Internal Server Error"
                                    )
                                    CrashHandler.report(cause)
                                    throw cause
                                }
                            }

                            routing {

                                get("/styles.css") {
                                    call.respondText(
                                        readTextFromAssets("styles.css"),
                                        ContentType.Text.CSS
                                    )
                                }
                                get("/favicon.ico") {
                                    call.respondBytes(
                                        readBytesFromAssets("favicon.ico"),
                                        ContentType.Image.XIcon
                                    )
                                }
                                get("messages.js") {
                                    call.respondText(
                                        """
                                    let messages = {
                                    ${i18nJson("app_name")},
                                    ${i18nJson("title_webui")},
                                    ${i18nJson("account")},
                                    ${i18nJson("amount")},
                                    ${i18nJson("date")},
                                    ${i18nJson("time")},
                                    ${i18nJson("booking_date")},
                                    ${i18nJson("value_date")},
                                    ${i18nJson("payer_or_payee")},
                                    ${i18nJson("category")},
                                    ${i18nJson("tags")},
                                    ${i18nJson("comment")},
                                    ${i18nJson("method")},
                                    ${i18nJson("menu_save")},
                                    ${i18nJson("menu_create_transaction")},
                                    ${i18nJson("menu_edit_transaction")},
                                    ${i18nJson("reference_number")},
                                    ${i18nJson("menu_edit")},
                                    ${i18nJson("dialog_confirm_discard_changes")},
                                    ${i18nJson("menu_clone_transaction")},
                                    ${i18nJson("menu_delete")},
                                    ${i18nJson("no_expenses")},
                                    ${i18nJson("webui_warning_move_transaction")}                                    ,
                                    ${i18nJsonPlurals("warning_delete_transaction")}
                                    };
                                """.trimIndent(), ContentType.Text.JavaScript
                                    )
                                }
                                if (passWord == null) {
                                    serve()
                                } else {
                                    authenticate("auth-basic") {
                                        serve()
                                    }
                                }
                            }
                        }
                    }

                    server = embeddedServer(if (useHttps) Netty else CIO, environment).also {
                        lifecycleScope.launch(Dispatchers.Default) {
                            it.start(wait = false)
                        }
                    }

                    val stopIntent = Intent(this, WebInputService::class.java).apply {
                        action = STOP_CLICK_ACTION
                    }
                    val notification: Notification =
                        NotificationBuilderWrapper.defaultBigTextStyleBuilder(
                            this,
                            getString(R.string.title_webui),
                            address
                        )
                            .addAction(
                                0,
                                0,
                                getString(R.string.stop),
                                //noinspection InlinedApi
                                PendingIntent.getService(
                                    this,
                                    0,
                                    stopIntent,
                                    FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                            .build()

                    startForeground(NOTIFICATION_WEB_UI, notification)
                    serverStateObserver?.postAddress(address)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun Route.serve() {

        get("data.js") {
            val categories = contentResolver.query(
                TransactionProvider.CATEGORIES_URI.buildUpon()
                    .appendQueryParameter(
                        TransactionProvider.QUERY_PARAMETER_HIERARCHICAL,
                        "1"
                    ).build(),
                arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL, KEY_LEVEL),
                null, null, null
            )?.use { cursor ->
                cursor.asSequence.map {
                    mapOf(
                        "id" to it.getLong(0),
                        "parent" to it.getLongOrNull(1),
                        "label" to it.getString(2),
                        "level" to it.getInt(3)
                    )
                }
                    .toList()
            }
            val data = mapOf(
                "accounts" to contentResolver.query(
                    TransactionProvider.ACCOUNTS_BASE_URI,
                    arrayOf(KEY_ROWID, KEY_LABEL, KEY_TYPE, KEY_CURRENCY),
                    "$KEY_SEALED = 0", null, null
                )?.use { cursor ->
                    cursor.asSequence.map {
                        mapOf(
                            "id" to it.getLong(0),
                            "label" to it.getString(1),
                            "type" to it.getString(2),
                            "currency" to currencyContext[it.getString(3)].symbol
                        )
                    }.toList()
                },
                "payees" to contentResolver.query(
                    TransactionProvider.PAYEES_URI,
                    arrayOf(KEY_ROWID, KEY_PAYEE_NAME),
                    null, null, null
                )?.use { cursor ->
                    cursor.asSequence.map {
                        mapOf(
                            "id" to it.getLong(0),
                            "name" to it.getString(1)
                        )
                    }
                        .toList()
                },
                "categories" to categories,
                "tags" to contentResolver.query(
                    TransactionProvider.TAGS_URI,
                    arrayOf(KEY_ROWID, KEY_LABEL),
                    null, null, null
                )?.use { cursor ->
                    cursor.asSequence.map {
                        mapOf(
                            "id" to it.getLong(0),
                            "label" to it.getString(1)
                        )
                    }
                        .toList()
                },
                "methods" to contentResolver.query(
                    TransactionProvider.METHODS_URI,
                    arrayOf(
                        KEY_ROWID,
                        KEY_LABEL,
                        KEY_IS_NUMBERED,
                        KEY_TYPE,
                        KEY_ACCOUNT_TPYE_LIST
                    ),
                    null, null, null
                )?.use { cursor ->
                    cursor.asSequence.map {
                        mapOf(
                            "id" to it.getLong(0),
                            "label" to it.getString(1),
                            "isNumbered" to (it.getInt(2) > 0),
                            "type" to it.getInt(3),
                            "accountTypes" to it.getString(4)?.split(',')
                        )
                    }.toList()
                },
            )
            val categoryTreeDepth = categories?.maxOfOrNull { it["level"] as Int } ?: 0
            val categoryWatchers = if (categoryTreeDepth > 1) {
                (0..categoryTreeDepth - 2).joinToString(separator = "\n") {
                    "this.\$watch('categoryPath[$it].id', value => { this.categoryPath[${it + 1}].id=0 } );"
                }
            } else ""
            val lookup = StringLookup { key ->
                when (key) {
                    "category_tree_depth" -> categoryTreeDepth.toString()
                    "data" -> gson.toJson(data)
                    "categoryWatchers" -> categoryWatchers
                    "withValueDate" -> prefHandler.getBoolean(
                        PrefKey.TRANSACTION_WITH_VALUE_DATE,
                        false
                    ).toString()
                    "withTime" -> prefHandler.getBoolean(
                        PrefKey.TRANSACTION_WITH_TIME,
                        false
                    ).toString()
                    else -> throw IllegalStateException("Unknown substitution key $key")
                }
            }
            val stringSubstitutor = StringSubstitutor(
                lookup,
                DEFAULT_PREFIX,
                DEFAULT_SUFFIX,
                DEFAULT_ESCAPE
            )
            val text = stringSubstitutor.replace(readTextFromAssets("data.js"))
            call.respondText(text, ContentType.Text.JavaScript)
        }

        delete("/transactions/{id}") {
            if (repository.deleteTransaction(call.parameters["id"]!!.toLong())) {
                call.respond(
                    HttpStatusCode.OK,
                    getString(R.string.transaction_deleted)
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Error while deleting transaction."
                )
            }
        }

        put("/transactions/{id}") {
            val transaction = call.receive<Transaction>()
            val updated = repository.updateTransaction(call.parameters["id"]!!, transaction)
            if (updated != null && updated > 0) {
                call.respond(
                    HttpStatusCode.OK,
                    getString(R.string.save_transaction_and_new_success)
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Error while saving transaction."
                )
            }
        }

        post("/transactions") {
            val transaction = call.receive<Transaction>()
            val id = repository.createTransaction(transaction)
            if (id != null) {
                call.response.headers.append(HttpHeaders.Location, "/transactions/$id")
                call.respond(
                    HttpStatusCode.Created,
                    getString(R.string.save_transaction_and_new_success)
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Error while saving transaction."
                )
            }
        }

        get("/") {
            call.respondText(readTextFromAssets("form.html"), ContentType.Text.Html)
        }

        get("/transactions") {
            call.respond(repository.loadTransactions(call.request.queryParameters["account_id"]!!.toLong()))
        }
    }

    private fun isAvailable(portNr: Int) = try {
        ServerSocket(portNr).close()
        true
    } catch (e: IOException) {
        false
    }

    private fun i18nJsonPlurals(resourceName: String, quantity: Int = 1) =
        "$resourceName : '${
            tqPlurals(
                resources.getIdentifier(resourceName, "plurals", packageName),
                quantity
            )
        }'"

    private fun i18nJson(resourceName: String) =
        "$resourceName : '${tq(resources.getIdentifier(resourceName, "string", packageName))}'"

    private fun tq(@StringRes resId: Int) = wrappedContext.getString(resId).replace("'", "\\'")

    private fun tqPlurals(@PluralsRes resId: Int, quantity: Int) =
        wrappedContext.resources.getQuantityString(resId, quantity, quantity).replace("'", "\\'")

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

    companion object {
        init {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }
}