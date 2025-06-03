package org.totschnig.myexpenses.service

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.runBlocking
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.instantiateTemplate
import org.totschnig.myexpenses.provider.DatabaseConstants
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

    @Deprecated("Deprecated in Java")
    override fun onCreate() {
        super.onCreate()
        (application as MyApplication).appComponent.inject(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        var message: String?
        if (intent == null) return
        val contentResolver = getContentResolver()
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
        val templateId = extras.getLong(DatabaseConstants.KEY_TEMPLATEID)
        val instanceId = extras.getLong(DatabaseConstants.KEY_INSTANCEID)
        when (action) {
            PlanExecutor.ACTION_APPLY -> {
                val date =
                    extras.getLong(DatabaseConstants.KEY_DATE, Instant.now().toEpochMilli())
                val t =
                    runBlocking {
                        instantiateTemplate(
                            repository,
                            exchangeRateHandler,
                            PlanInstanceInfo(templateId, instanceId, date),
                            currencyContext.homeCurrencyUnit
                        )
                    }
                if (t != null) {
                    message = resources.getQuantityString(
                        R.plurals.save_transaction_from_template_success, 1, 1
                    )
                    val displayIntent = Intent(this, MyExpenses::class.java)
                        .putExtra(DatabaseConstants.KEY_ROWID, t.accountId)
                        .putExtra(DatabaseConstants.KEY_TRANSACTIONID, t.id)
                    val resultIntent = PendingIntent.getActivity(
                        this, notificationId, displayIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setContentIntent(resultIntent)
                    builder.setAutoCancel(true)
                } else {
                    message = getString(R.string.save_transaction_error)
                }
            }

            PlanExecutor.ACTION_CANCEL -> {
                val values = ContentValues()
                values.putNull(DatabaseConstants.KEY_TRANSACTIONID)
                values.put(DatabaseConstants.KEY_TEMPLATEID, templateId)
                values.put(DatabaseConstants.KEY_INSTANCEID, instanceId)
                try {
                    getContentResolver().insert(
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
