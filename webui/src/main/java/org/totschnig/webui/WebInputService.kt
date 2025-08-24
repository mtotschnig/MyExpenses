package org.totschnig.webui

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.app.ServiceCompat
import androidx.core.database.getLongOrNull
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.StringSubstitutor.DEFAULT_ESCAPE
import org.apache.commons.text.StringSubstitutor.DEFAULT_PREFIX
import org.apache.commons.text.StringSubstitutor.DEFAULT_SUFFIX
import org.apache.commons.text.lookup.StringLookup
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.db2.updateTransaction
import org.totschnig.myexpenses.di.LocalDateAdapter
import org.totschnig.myexpenses.di.LocalTimeAdapter
import org.totschnig.myexpenses.feature.IWebInputService
import org.totschnig.myexpenses.feature.RESTART_ACTION
import org.totschnig.myexpenses.feature.START_ACTION
import org.totschnig.myexpenses.feature.STOP_ACTION
import org.totschnig.myexpenses.feature.ServerStateObserver
import org.totschnig.myexpenses.feature.WebUiBinder
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LEVEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.FileInfo
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_WEB_UI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.getActiveIpAddress
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.io.IOException
import java.lang.ref.WeakReference
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
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    private lateinit var wrappedContext: Context

    private var serverStateObserver: ServerStateObserver? = null

    private var port: Int = 0

    class LocalBinder(private val webInputService: WeakReference<WebInputService>) : WebUiBinder() {
        override fun getService() = webInputService.get()
    }

    override fun onCreate() {
        super.onCreate()
        DaggerWebUiComponent.builder().appComponent((application as MyApplication).appComponent)
            .build().inject(this)
        wrappedContext = (application as MyApplication).wrapContext(this)
        //This shows the notification also if we bind to the service from WebUiViewModel.
        //unfortunately I am not aware of any means to distinguish between being created in the context
        //bindService or startService
/*        val notification: Notification =
            NotificationBuilderWrapper.defaultBigTextStyleBuilder(
                this,
                getString(R.string.title_webui),
                "Starting ..."
            ).build()
        startForeground(NOTIFICATION_WEB_UI, notification)*/
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return LocalBinder(WeakReference(this))
    }

    private var server: EmbeddedServer<*,*>? = null

    private var serverAddress: String? = null

    override fun registerObserver(serverStateObserver: ServerStateObserver) {
        this.serverStateObserver = serverStateObserver
        serverAddress?.let { serverStateObserver.postAddress(it) }
    }

    override fun unregisterObserver() {
        this.serverStateObserver = null
    }

    private fun getProtocol(useHttps: Boolean): String = if (useHttps) "https" else "http"

    private fun getAddress(useHttps: Boolean) = getActiveIpAddress(this)?.let {
        "${getProtocol(useHttps)}://$it:$port"
    } ?: "Listening on port $port"


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

            START_ACTION, RESTART_ACTION -> {
                licenceHandler.recordUsage(ContribFeature.WEB_UI)
                if (server != null) {
                    if (intent.action == START_ACTION) return START_STICKY
                    stopServer()
                }
                if (try {
                        (9000..9050).first { isAvailable(it) }
                    } catch (_: NoSuchElementException) {
                        serverStateObserver?.postException(IOException("No available port found in range 9000..9050"))
                        0
                    }.let { port = it; it != 0 }
                ) {
                    try {
                        val useHttps = prefHandler.getBoolean(PrefKey.WEBUI_HTTPS, false)
                        server = embeddedServer(if (useHttps) Netty else CIO,
                            configure = {
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
                            },
                            module = {
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
                                        ${i18nJson("app_name", R.string.app_name)},
                                        ${i18nJson("title_webui", R.string.title_webui)},
                                        ${i18nJson("account", R.string.account)},
                                        ${i18nJson("amount", R.string.amount)},
                                        ${i18nJson("date", R.string.date)},
                                        ${i18nJson("time", R.string.time)},
                                        ${i18nJson("booking_date", R.string.booking_date)},
                                        ${i18nJson("value_date", R.string.value_date)},
                                        ${i18nJson("payer_or_payee", R.string.payer_or_payee)},
                                        ${i18nJson("category", R.string.category)},
                                        ${i18nJson("tags", R.string.tags)},
                                        ${i18nJson("comment", R.string.notes)},
                                        ${i18nJson("method", R.string.method)},
                                        ${i18nJson("menu_save", R.string.menu_save)},
                                        ${i18nJson("menu_create_transaction", R.string.menu_create_transaction)},
                                        ${i18nJson("menu_edit_transaction", R.string.menu_edit_transaction)},
                                        ${i18nJson("reference_number", R.string.reference_number)},
                                        ${i18nJson("menu_edit", R.string.menu_edit)},
                                        ${i18nJson("dialog_confirm_discard_changes", R.string.dialog_confirm_discard_changes)},
                                        ${i18nJson("menu_clone_transaction", R.string.menu_clone_transaction)},
                                        ${i18nJson("menu_delete", R.string.menu_delete)},
                                        ${i18nJson("no_expenses", R.string.no_expenses)},
                                        ${i18nJson("webui_warning_move_transaction", R.string.webui_warning_move_transaction)},
                                        ${i18nJson("validate_error_not_empty", R.string.validate_error_not_empty)},
                                        ${i18nJsonPlurals("warning_delete_transaction", R.plurals.warning_delete_transaction)},
                                        ${i18nJson("action_download", R.string.action_download)}
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
                        ).also {
                            lifecycleScope.launch(Dispatchers.Default + ktorServerExceptionHandler) {
                                it.start(wait = false)
                            }
                        }

                        val stopIntent = Intent(this, WebInputService::class.java).apply {
                            action = STOP_CLICK_ACTION
                        }
                        serverAddress = getAddress(useHttps).also {
                            val notification: Notification =
                                NotificationBuilderWrapper.defaultBigTextStyleBuilder(
                                    this,
                                    getString(R.string.title_webui),
                                    it
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
                            serverStateObserver?.postAddress(it)
                        }
                    } catch (e: Throwable) {
                        handleException(e)
                    }
                }
            }
        }
        return START_STICKY
    }

    private val ktorServerExceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
    }

    private fun handleException(e: Throwable) {
        CrashHandler.report(e)
        serverStateObserver?.postException(e)
        prefHandler.putBoolean(PrefKey.UI_WEB, false)
        stopSelf()
    }

    private fun Route.serve() {

        get("data.js") {
            //noinspection Recycle
            val categories = contentResolver.query(
                TransactionProvider.CATEGORIES_URI.buildUpon()
                    .appendBooleanQueryParameter(
                        TransactionProvider.QUERY_PARAMETER_HIERARCHICAL
                    ).build(),
                arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL, KEY_LEVEL),
                null, null, null
            )?.useAndMapToList {
                mapOf(
                    "id" to it.getLong(0),
                    "parent" to it.getLongOrNull(1),
                    "label" to it.getString(2),
                    "level" to it.getInt(3)
                )
            }
            val data = mapOf(
                //noinspection Recycle
                "accounts" to contentResolver.query(
                    TransactionProvider.ACCOUNTS_BASE_URI,
                    arrayOf(KEY_ROWID, KEY_LABEL, KEY_TYPE, KEY_CURRENCY),
                    "$KEY_SEALED = 0", null, null
                )?.useAndMapToList {
                    mapOf(
                        "id" to it.getLong(0),
                        "label" to it.getString(1),
                        "type" to it.getString(2),
                        "currency" to currencyContext[it.getString(3)].symbol
                    )
                },
                //noinspection Recycle
                "payees" to contentResolver.query(
                    TransactionProvider.PAYEES_URI,
                    arrayOf(KEY_ROWID, KEY_PAYEE_NAME),
                    "$KEY_PARENTID IS  NULL", null, null
                )?.useAndMapToList {
                    mapOf(
                        "id" to it.getLong(0),
                        "name" to it.getString(1)
                    )
                },
                "categories" to categories,
                //noinspection Recycle
                "tags" to contentResolver.query(
                    TransactionProvider.TAGS_URI,
                    arrayOf(KEY_ROWID, KEY_LABEL),
                    null, null, null
                )?.useAndMapToList {
                    mapOf(
                        "id" to it.getLong(0),
                        "label" to it.getString(1)
                    )
                },
                //noinspection Recycle
                "methods" to contentResolver.query(
                    TransactionProvider.METHODS_URI,
                    arrayOf(
                        KEY_ROWID,
                        KEY_LABEL,
                        KEY_IS_NUMBERED,
                        KEY_TYPE,
                        KEY_ACCOUNT_TYPE_LIST
                    ),
                    null, null, null
                )?.useAndMapToList {
                    mapOf(
                        "id" to it.getLong(0),
                        "label" to it.getString(1),
                        "isNumbered" to (it.getInt(2) > 0),
                        "type" to it.getInt(3),
                        "accountTypes" to it.getString(4)?.split(',')
                    )
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
            val updated = repository.updateTransaction(call.parameters["id"]!!.toLong(), transaction)
            if (updated) {
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
            call.response.headers.append(HttpHeaders.Location, "/transactions/$id")
            call.respond(
                HttpStatusCode.Created,
                getString(R.string.save_transaction_and_new_success)
            )
        }

        get("/") {
            call.respondText(readTextFromAssets("form.html"), ContentType.Text.Html)
        }

        get("/transactions") {
            call.respond(repository.loadTransactions(call.request.queryParameters["account_id"]!!.toLong()))
        }

        get("/download") {
            val lookup = StringLookup { key ->
                when (key) {
                    "data" -> {
                        gson.toJson(
                            AppDirHelper
                                .getAppDirFiles(this@WebInputService)
                                .getOrThrow()
                                .map<FileInfo, Map<String, String>> { it ->
                                    mapOf(
                                        "name" to it.format(this@WebInputService),
                                        "link" to "download/${it.name}"
                                    )
                                }
                        )
                    }
                    "no_results" -> getString(R.string.webui_download_no_data)
                    else -> throw IllegalStateException("Unknown substitution key $key")
                }
            }
            val stringSubstitutor = StringSubstitutor(
                lookup,
                DEFAULT_PREFIX,
                DEFAULT_SUFFIX,
                DEFAULT_ESCAPE
            )
            val text = stringSubstitutor.replace(readTextFromAssets("download.html"))
            call.respondText(text, ContentType.Text.Html)
        }

        get("/download/{file}") {
            val appDir = AppDirHelper.getAppDir(this@WebInputService).getOrThrow()
            val file = appDir.findFile(call.parameters["file"]!!)?.uri?.let {
                AppDirHelper.ensureContentUri(it, this@WebInputService)
            }
            if (file == null) {
                call.respond(HttpStatusCode.NotFound, "File not found")
            } else {
                call.respond(
                    contentResolver.openInputStream(file)!!.toByteReadChannel()
                )
            }
        }
    }

    private fun isAvailable(portNr: Int) = try {
        ServerSocket(portNr).close()
        true
    } catch (_: IOException) {
        false
    }

    private fun i18nJsonPlurals(resourceName: String, @PluralsRes resourceId: Int, quantity: Int = 1) =
        "$resourceName : '${tqPlurals(resourceId, quantity)}'"

    private fun i18nJson(resourceName: String, @StringRes resourceId: Int) =
        "$resourceName : '${tq(resourceId)}'"

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
        serverAddress = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        true
    } else false

    companion object {
        init {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }
}