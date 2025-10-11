package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.ManageTemplates
import org.totschnig.myexpenses.activity.showTemplateInstantiationResult
import org.totschnig.myexpenses.db2.instantiateTemplate
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.doAsync
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo

const val CLICK_ACTION_SAVE = "save"
const val CLICK_ACTION_EDIT = "edit"

class TemplateWidget : AbstractListWidget(
    TemplateWidgetService::class.java,
    PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET
) {
    override val emptyTextResourceId = R.string.no_templates

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
                if ((context.myApplication).shouldLock(null)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.warning_instantiate_template_from_widget_password_protected),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    doAsync {
                        context.showTemplateInstantiationResult(
                            if (repository.instantiateTemplate(
                                exchangeRateHandler,
                                PlanInstanceInfo(templateId),
                                currencyContext
                            ) == null) 0 else 1
                        )
                    }
                }
            }

            CLICK_ACTION_EDIT -> context.startActivity(
                Intent(
                    context,
                    ExpenseEdit::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = ExpenseEdit.ACTION_CREATE_FROM_TEMPLATE
                    putExtra(DatabaseConstants.KEY_TEMPLATEID, templateId)
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