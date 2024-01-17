package org.totschnig.myexpenses.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForTemplates
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.requireLong
import org.totschnig.myexpenses.util.formatMoney
import java.util.Locale
import javax.inject.Inject

class TemplateWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TemplateRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TemplateRemoteViewsFactory(
    val context: Context,
    intent: Intent
) : AbstractRemoteViewsFactory(context, intent) {

    init {
        context.injector.inject(this)
    }

    override fun buildCursor(): Cursor? {
        return context.contentResolver.query(
            TransactionProvider.TEMPLATES_URI, null, String.format(
                Locale.ROOT, "%s is null AND %s is null AND %s = 0",
                KEY_PLANID, KEY_PARENTID, KEY_SEALED
            ),
            null, preferredOrderByForTemplates(prefHandler, Sort.TITLE, prefHandler.collate)
        )
    }

    override fun RemoteViews.populate(cursor: Cursor) {
        setBackgroundColorSave(
            R.id.divider3,
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COLOR))
        )
        val title = cursor.getString(KEY_TITLE)
        val currencyContext = context.injector.currencyContext()
        val currency = currencyContext.get(cursor.getString(KEY_CURRENCY))
        val amount = Money(currency, cursor.requireLong(KEY_AMOUNT))
        val isTransfer = !(cursor.isNull(cursor.getColumnIndexOrThrow(KEY_TRANSFER_ACCOUNT)))

        val comment = cursor.getString(KEY_COMMENT)
        val payee = cursor.getString(KEY_PAYEE_NAME)
        setTextViewText(
            R.id.line1,
            title + " : " + context.injector.currencyFormatter().formatMoney(amount)
        )
        val commentSeparator = " / "
        val description = SpannableStringBuilder(
            if (isTransfer) {
                val accountLabel = cursor.getStringOrNull(KEY_ACCOUNT_LABEL)
                val transferAccountLabel = cursor.getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL)
                if (amount.amountMinor < 0) "$accountLabel ${Transfer.RIGHT_ARROW} $transferAccountLabel"
                else "$transferAccountLabel ${Transfer.RIGHT_ARROW} $accountLabel"
            } else cursor.getString(KEY_PATH)
        )
        if (!TextUtils.isEmpty(comment)) {
            if (description.isNotEmpty()) {
                description.append(commentSeparator)
            }
            description.append(comment)
            val before = description.length
            description.setSpan(
                StyleSpan(Typeface.ITALIC), before, description.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (!TextUtils.isEmpty(payee)) {
            if (description.isNotEmpty()) {
                description.append(commentSeparator)
            }
            description.append(payee)
            val before = description.length
            description.setSpan(
                UnderlineSpan(), before, description.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        setTextViewText(R.id.note, description)
        setOnClickFillInIntent(R.id.object_info, Intent())
        val templateId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
        configureButton(
            R.id.command1,
            R.drawable.ic_action_apply_save,
            CLICK_ACTION_SAVE,
            R.string.menu_create_instance_save,
            templateId,
            1
        )
        configureButton(
            R.id.command2,
            R.drawable.ic_action_apply_edit,
            CLICK_ACTION_EDIT,
            R.string.menu_create_instance_edit,
            templateId,
            2
        )
        setViewVisibility(R.id.command3, View.GONE)
    }

    private fun RemoteViews.configureButton(
        buttonId: Int,
        drawableResId: Int,
        action: String,
        contentDescriptionResId: Int,
        templateId: Long,
        position: Int
    ) {
        if (width < 48 * position) {
            setViewVisibility(buttonId, View.GONE)
        } else {
            setViewVisibility(buttonId, View.VISIBLE)
            setImageViewResource(buttonId, drawableResId)
            setContentDescription(buttonId, context.getString(contentDescriptionResId))
            setOnClickFillInIntent(buttonId, Intent().apply {
                putExtra(KEY_ROWID, templateId)
                putExtra(KEY_CLICK_ACTION, action)
            })
        }
    }
}