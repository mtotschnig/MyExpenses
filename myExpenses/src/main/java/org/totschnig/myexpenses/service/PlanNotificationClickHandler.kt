package org.totschnig.myexpenses.service

import android.app.IntentService
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.runBlocking
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.instantiateTemplate
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import java.time.Instant
import javax.inject.Inject

//TODO migrate to WorkManager
class PlanNotificationClickHandler : IntentService("PlanNotificationClickHandler") {

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var prefHandler: PrefHandler

    @Deprecated("Deprecated in Java")
    override fun onCreate() {
        super.onCreate()
        (application as MyApplication).appComponent.inject(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        var message: String?
        if (intent == null) return
        val extras = intent.extras
        val action = intent.action
        if (extras == null || action == null) return
        val title = extras.getString(PlanExecutor.KEY_TITLE)
        val builder = NotificationBuilderWrapper(
            this,
            NotificationBuilderWrapper.CHANNEL_ID_PLANNER
        )
            .setSmallIcon(R.drawable.ic_stat_notification_sigma)
            .setContentTitle(title)
        val notificationId = extras.getInt(MyApplication.KEY_NOTIFICATION_ID)
        val templateId = extras.getLong(KEY_TEMPLATEID)
        val instanceId = extras.getLong(KEY_INSTANCEID)
        when (action) {
            PlanExecutor.ACTION_APPLY -> {
                val date =
                    extras.getLong(KEY_DATE, Instant.now().toEpochMilli())
                val t = runBlocking {
                    repository.instantiateTemplate(
                        exchangeRateHandler,
                        PlanInstanceInfo(templateId, instanceId, date),
                        currencyContext
                    )
                }
                if (t != null) {
                    message = resources.getQuantityString(
                        R.plurals.save_transaction_from_template_success, 1, 1
                    )
                    val resultIntent = prefHandler.createShowDetailsIntent(applicationContext, notificationId, t.data)

                    builder.setContentIntent(resultIntent)
                    builder.setAutoCancel(true)
                } else {
                    message = getString(R.string.save_transaction_error)
                }
            }

            PlanExecutor.ACTION_CANCEL -> {
                val values = ContentValues()
                values.putNull(KEY_TRANSACTIONID)
                values.put(KEY_TEMPLATEID, templateId)
                values.put(KEY_INSTANCEID, instanceId)
                try {
                    contentResolver.insert(
                        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
                        values
                    )
                    message = getString(R.string.plan_execution_canceled)
                } catch (_: SQLiteConstraintException) {
                    message = getString(R.string.save_transaction_template_deleted)
                }
            }

            else -> {
                return
            }
        }
        builder.setContentText(message)
        (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)?.notify(
            notificationId,
            builder.build()
        )
    }
}
