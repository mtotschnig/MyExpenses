package org.totschnig.myexpenses.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.text.TextUtils
import androidx.core.content.getSystemService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.util.TextUtils.concatResStrings

object ContribUtils {
    @JvmStatic
    fun showContribNotification(context: Context, feature: ContribFeature) {
        val notifTitle = concatResStrings(
            context,
            R.string.app_name,
            feature.labelResId
        )
        val content = TextUtils.concat(
            feature.trialExpiredWarning(context),
            " ",
            feature.upgradeInfo(context, true)
        )

        val contribIntent = ContribInfoDialogActivity.getIntentFor(context, feature)
        val notification = NotificationBuilderWrapper
            .defaultBigTextStyleBuilder(context, notifTitle, content)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    contribIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(NotificationBuilderWrapper.NOTIFICATION_CONTRIB, notification)
    }
}
