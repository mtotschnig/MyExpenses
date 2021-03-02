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
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForTemplates
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import java.util.*
import javax.inject.Inject


class TemplateWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TemplatetRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TemplatetRemoteViewsFactory(
        val context: Context,
        intent: Intent
) : AbstractRemoteViewsFactory(context, intent) {
    @Inject
    lateinit var prefHandler: PrefHandler

    init {
        (context.applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun buildCursor(): Cursor? {
        return context.contentResolver.query(
                TransactionProvider.TEMPLATES_URI, null, String.format(Locale.ROOT, "%s is null AND %s is null AND %s = 0",
                KEY_PLANID, KEY_PARENTID, KEY_SEALED),
                null, preferredOrderByForTemplates(prefHandler, Sort.TITLE))
    }

    override fun RemoteViews.populate(cursor: Cursor) {
        setBackgroundColorSave(R.id.divider3, cursor.getInt(cursor.getColumnIndex(KEY_COLOR)))
        val title = DbUtils.getString(cursor, DatabaseConstants.KEY_TITLE)
        val currencyContext = MyApplication.getInstance().appComponent.currencyContext()
        val currency = currencyContext.get(DbUtils.getString(cursor, KEY_CURRENCY))
        val amount = Money(currency, DbUtils.getLongOr0L(cursor, DatabaseConstants.KEY_AMOUNT))
        val isTransfer = !(cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_TRANSFER_ACCOUNT)))
        val label = DbUtils.getString(cursor, DatabaseConstants.KEY_LABEL)
        val comment = DbUtils.getString(cursor, DatabaseConstants.KEY_COMMENT)
        val payee = DbUtils.getString(cursor, DatabaseConstants.KEY_PAYEE_NAME)
        setTextViewText(R.id.line1,
                title + " : " + (context.applicationContext as MyApplication).appComponent.currencyFormatter().formatCurrency(amount))
        val commentSeparator = " / "
        val description = SpannableStringBuilder(if (isTransfer) Transfer.getIndicatorPrefixForLabel(amount.amountMinor) + label else label)
        if (!TextUtils.isEmpty(comment)) {
            if (description.isNotEmpty()) {
                description.append(commentSeparator)
            }
            description.append(comment)
            val before = description.length
            description.setSpan(StyleSpan(Typeface.ITALIC), before, description.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (!TextUtils.isEmpty(payee)) {
            if (description.isNotEmpty()) {
                description.append(commentSeparator)
            }
            description.append(payee)
            val before = description.length
            description.setSpan(UnderlineSpan(), before, description.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setTextViewText(R.id.note, description)
        setOnClickFillInIntent(R.id.object_info, Intent())
        val templateId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
        configureButton(R.id.command1, R.drawable.ic_action_apply_save, CLICK_ACTION_SAVE, R.string.menu_create_instance_save, templateId, 175)
        configureButton(R.id.command2, R.drawable.ic_action_apply_edit, CLICK_ACTION_EDIT, R.string.menu_create_instance_edit, templateId, 223)
        setViewVisibility(R.id.command3, View.GONE)
    }

    private fun RemoteViews.configureButton(buttonId: Int, drawableResId: Int, action: String, contentDescriptionResId: Int, templateId: Long, minimumWidth: Int) {
        if (width < minimumWidth) {
            setViewVisibility(buttonId, View.GONE)
        } else {
            setViewVisibility(buttonId, View.VISIBLE)
            setImageViewVectorDrawable(buttonId, drawableResId)
            setContentDescription(buttonId, context.getString(contentDescriptionResId))
            setOnClickFillInIntent(buttonId, Intent().apply {
                putExtra(KEY_ROWID, templateId)
                putExtra(KEY_CLICK_ACTION, action)
            })
        }
    }
}