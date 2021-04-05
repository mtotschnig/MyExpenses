package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

const val CLICK_ACTION_SAVE = "save"
const val CLICK_ACTION_EDIT = "edit"

class TemplateWidget: AbstractWidget(TemplateWidgetService::class.java, PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) {
    override fun emptyTextResourceId(context: Context, appWidgetId: Int) = R.string.no_templates

    override fun handleWidgetClick(context: Context, intent: Intent) {
        val templateId = intent.getLongExtra(DatabaseConstants.KEY_ROWID, 0)
        when (intent.getStringExtra(KEY_CLICK_ACTION)) {
            null -> {
                context.startActivity(Intent(context, ManageTemplates::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_START_FROM_WIDGET, true)
                })
            }
            CLICK_ACTION_SAVE -> {
                if (MyApplication.getInstance().shouldLock(null)) {
                    Toast.makeText(context,
                            context.getString(R.string.warning_instantiate_template_from_widget_password_protected),
                            Toast.LENGTH_LONG).show()
                } else {
                    Transaction.getInstanceFromTemplateWithTags(templateId)?.let {
                        if (it.first!!.save(true) != null && it.first!!.saveTags(it.second, context.contentResolver)) {
                            Toast.makeText(context,
                                    context.resources.getQuantityString(R.plurals.save_transaction_from_template_success, 1, 1),
                                    Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            CLICK_ACTION_EDIT -> context.startActivity(Intent(context, ExpenseEdit::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DatabaseConstants.KEY_TEMPLATEID, templateId)
                putExtra(DatabaseConstants.KEY_INSTANCEID, -1L)
                putExtra(EXTRA_START_FROM_WIDGET, true)
                putExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
            })
        }
    }

    companion object {
        val OBSERVED_URIS = arrayOf(
                TransactionProvider.TEMPLATES_URI,
                TransactionProvider.ACCOUNTS_URI //if color changes
        )

    }
}