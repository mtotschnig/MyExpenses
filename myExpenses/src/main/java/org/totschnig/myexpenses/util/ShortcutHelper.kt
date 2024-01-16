package org.totschnig.myexpenses.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity.Companion.getIntentFor
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.SimpleToastActivity
import org.totschnig.myexpenses.activity.TemplateSaver
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Template.Action
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.TemplateInfo
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY

object ShortcutHelper {
    const val ID_TRANSACTION = "transaction"
    const val ID_TRANSFER = "transfer"
    const val ID_SPLIT = "split"
    fun idTemplate(templateId: Long) = "template-$templateId"

    fun createIntentForNewSplit(context: Context) =
        createIntentForNewTransaction(context, Transactions.TYPE_SPLIT)

    private fun createIntentForNewTransfer(context: Context) =
        createIntentForNewTransaction(context, Transactions.TYPE_TRANSFER)

    fun createIntentForNewTransaction(context: Context, operationType: Int) =
        Intent().apply {
            action = Intent.ACTION_MAIN
            component = ComponentName(
                context.packageName,
                ExpenseEdit::class.java.name
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

            putExtras(Bundle().apply {
                putBoolean(EXTRA_START_FROM_WIDGET, true)
                putBoolean(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
                putInt(Transactions.OPERATION_TYPE, operationType)
                putBoolean(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
            })
        }

    fun configureSplitShortcut(context: Context, contribEnabled: Boolean) {
        val intent: Intent = if (contribEnabled) {
            createIntentForNewSplit(context)
        } else {
            getIntentFor(context, ContribFeature.SPLIT_TRANSACTION)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, ID_SPLIT)
            .setShortLabel(context.getString(R.string.split_transaction))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_menu_split_shortcut))
            .setIntent(intent)
            .build()
        try {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Exception) {
            CrashHandler.report(e)
        }
    }

    fun configureTransferShortcut(context: Context, transferEnabled: Boolean) {
        val intent: Intent = if (transferEnabled) {
            createIntentForNewTransfer(context)
        } else {
            Intent(context, SimpleToastActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(
                    SimpleToastActivity.KEY_MESSAGE,
                    context.getString(R.string.dialog_command_disabled_insert_transfer)
                )
        }
        val shortcut = ShortcutInfoCompat.Builder(context, ID_TRANSFER)
            .setShortLabel(context.getString(R.string.transfer))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_menu_forward_shortcut))
            .setIntent(intent)
            .build()
        try {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Exception) {
            CrashHandler.report(e)
        }
    }

    fun buildTemplateIntent(context: Context, id: Long, templateAction: Action) =
        if (templateAction == Action.SAVE)
            Intent(context, TemplateSaver::class.java).apply {
                action = Intent.ACTION_INSERT
                putExtra(DatabaseConstants.KEY_TEMPLATEID, id)
            } else
            Intent(context, ExpenseEdit::class.java).apply {
                action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DatabaseConstants.KEY_TEMPLATEID, id)
                putExtra(EXTRA_START_FROM_WIDGET, true)
                putExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
            }

    fun buildTemplateShortcut(context: Context, templateInfo: TemplateInfo): ShortcutInfoCompat {
        val title = templateInfo.title.takeIf { it.isNotEmpty() } ?: context.getString(R.string.template)
        return ShortcutInfoCompat.Builder(context, idTemplate(templateInfo.rowId))
            .setShortLabel(title)
            .setLongLabel(title)
            .setIntent(buildTemplateIntent(context, templateInfo.rowId, templateInfo.defaultAction))
            .setIcon(
                IconCompat.createWithResource(
                    context,
                    R.drawable.ic_menu_template_shortcut
                )
            )
            .build()
    }
}