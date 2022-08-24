package org.totschnig.myexpenses.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.cursoradapter.widget.ResourceCursorAdapter
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.fragment.BaseTransactionList.COMMENT_SEPARATOR
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.Category
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

open class TransactionAdapter(
    private val groupingOverride: Grouping?,
    private val context: Context,
    layout: Int,
    c: Cursor?,
    flags: Int,
    private val currencyFormatter: CurrencyFormatter,
    private val prefHandler: PrefHandler,
    private val currencyContext: CurrencyContext,
    private val onToggleCrStatus: OnToggleCrStatus?
) : ResourceCursorAdapter(
    context, layout, c, flags
) {
    private var dateEms = 0
    private val is24HourFormat = android.text.format.DateFormat.is24HourFormat(context)
    private var shouldShowTime = false
    private lateinit var mAccount: Account
    private val localizedTimeFormat = android.text.format.DateFormat.getTimeFormat(context)
    private var itemDateFormat: DateFormat? = null
    private val colorExpense =
        ResourcesCompat.getColor(context.resources, R.color.colorExpense, null)
    private val colorIncome =
        ResourcesCompat.getColor(context.resources, R.color.colorIncome, null)
    private val textColorSecondary = (context as ProtectedFragmentActivity).textColorSecondary
    private val monthStart = prefHandler.getString(PrefKey.GROUP_MONTH_STARTS, "1")!!.toInt()
    private var futureCriterion: Long = 0

    interface OnToggleCrStatus {
        fun toggle(id: Long)
    }

    constructor(
        context: Context, layout: Int, c: Cursor?, flags: Int,
        currencyFormatter: CurrencyFormatter, prefHandler: PrefHandler,
        currencyContext: CurrencyContext, onToggleCrStatus: OnToggleCrStatus?
    ) : this(
        null,
        context,
        layout,
        c,
        flags,
        currencyFormatter,
        prefHandler,
        currencyContext,
        onToggleCrStatus
    )

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val v = super.newView(context, cursor, parent)
        val holder = ViewHolder(v)
        if (mAccount.id < 0) {
            holder.colorAccount.visibility = View.VISIBLE
        }
        v.tag = holder
        return v
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val viewHolder = view.tag as ViewHolder
        viewHolder.date.setEms(dateEms)
        val date = cursor.getLong(KEY_DATE)
        (view as FrameLayout).foreground =
            if (date >= futureCriterion) ColorDrawable(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.future_background,
                    null
                )
            ) else null
        viewHolder.date.text =
            if (itemDateFormat != null) Utils.convDateTime(date, itemDateFormat) else null
        val isTransfer = cursor.getLongOrNull(KEY_TRANSFER_PEER) != null
        //for the Grand Total account, we show equivalent amounts in the home currency for normal transactions
        //but show transfers in there real currency
        val columnIndexCurrency = cursor.getColumnIndex(KEY_CURRENCY)
        val currency = if (isTransfer && columnIndexCurrency > -1) currencyContext[cursor.getString(
            columnIndexCurrency
        )] else mAccount.currencyUnit
        val columnIndexEquivalentAmount = cursor.getColumnIndex(KEY_EQUIVALENT_AMOUNT)
        val amount =
            cursor.getLong(
                if (isTransfer || columnIndexEquivalentAmount == -1)
                    cursor.getColumnIndexOrThrow(KEY_AMOUNT) else columnIndexEquivalentAmount
            )
        val tv1 = viewHolder.amount
        tv1.text = currencyFormatter.convAmount(amount, currency)
        tv1.setTextColor(if (amount < 0) colorExpense else colorIncome)
        if (mAccount.isAggregate) {
            val columnIndexSameCurrency =
                cursor.getColumnIndex(KEY_IS_SAME_CURRENCY)
            if (columnIndexSameCurrency == -1 || cursor.getInt(columnIndexSameCurrency) != 1) {
                val color = cursor.getInt(KEY_COLOR)
                viewHolder.colorAccount.setBackgroundColor(color)
            } else {
                viewHolder.colorAccount.setBackgroundColor(0)
                tv1.setTextColor(textColorSecondary)
            }
        }
        val tv2 = viewHolder.category
        var catText: CharSequence = cursor.getString(KEY_LABEL)
        if (isTransfer) {
            catText = Transfer.getIndicatorPrefixForLabel(amount) + catText
            if (mAccount.isAggregate) {
                catText = cursor.getString(KEY_ACCOUNT_LABEL) + " " + catText
            }
        } else {
            val catId = DbUtils.getLongOrNull(cursor, KEY_CATID)
            if (SPLIT_CATID == catId) catText =
                context.getString(R.string.split_transaction) else if (catId == null) {
                if (cursor.getInt(KEY_STATUS) != STATUS_HELPER) {
                    catText = Category.NO_CATEGORY_ASSIGNED_LABEL
                }
            }
        }
        val referenceNumber = cursor.getStringOrNull(KEY_REFERENCE_NUMBER)
        if (referenceNumber != null && referenceNumber.isNotEmpty()) catText =
            "($referenceNumber) $catText"
        var ssb: SpannableStringBuilder
        val comment = cursor.getStringOrNull(KEY_COMMENT)
        if (comment != null && comment.isNotEmpty()) {
            ssb = SpannableStringBuilder(comment)
            ssb.setSpan(StyleSpan(Typeface.ITALIC), 0, comment.length, 0)
            catText = if (catText.isNotEmpty()) TextUtils.concat(
                catText,
                COMMENT_SEPARATOR,
                ssb
            ) else ssb
        }
        val payee = cursor.getStringOrNull(KEY_PAYEE_NAME)
        if (payee != null && payee.isNotEmpty()) {
            ssb = SpannableStringBuilder(payee)
            ssb.setSpan(UnderlineSpan(), 0, payee.length, 0)
            catText = if (catText.isNotEmpty()) TextUtils.concat(
                catText,
                COMMENT_SEPARATOR,
                ssb
            ) else ssb
        }
        val tagList = cursor.getStringOrNull(KEY_TAGLIST)
        if (tagList != null && tagList.isNotEmpty()) {
            ssb = SpannableStringBuilder(tagList)
            ssb.setSpan(StyleSpan(Typeface.BOLD), 0, tagList.length, 0)
            catText = if (catText.isNotEmpty()) TextUtils.concat(
                catText,
                COMMENT_SEPARATOR,
                ssb
            ) else ssb
        }
        tv2.text = catText
        val status: CrStatus =
            enumValueOrDefault(cursor.getString(KEY_CR_STATUS), CrStatus.UNRECONCILED)

        if (onToggleCrStatus == null || cursor.getString(KEY_ACCOUNT_TYPE) == AccountType.CASH.name || status == CrStatus.VOID) {
            viewHolder.colorContainer.visibility = View.GONE
        } else {
            viewHolder.color1.setBackgroundColor(status.color)
            viewHolder.colorContainer.tag =
                if (status == CrStatus.RECONCILED) -1 else cursor.getLong(KEY_ROWID)
            viewHolder.colorContainer.visibility = View.VISIBLE
        }
        viewHolder.voidMarker.visibility =
            if (status == CrStatus.VOID) View.VISIBLE else View.GONE
    }

    private fun localeFromContext(): Locale {
        return Utils.localeFromContext(context)
    }

    fun refreshDateFormat() {

        dateEms = 3

        when (groupingOverride ?: mAccount.grouping) {
            Grouping.DAY -> if (shouldShowTime) {
                itemDateFormat = localizedTimeFormat
                dateEms = if (is24HourFormat) 3 else 4
            } else {
                itemDateFormat = null
                dateEms = 0
            }
            Grouping.MONTH -> if (monthStart == 1) {
                itemDateFormat = SimpleDateFormat("dd", localeFromContext())
                dateEms = 2
            } else {
                itemDateFormat = Utils.localizedYearLessDateFormat(context)
            }
            Grouping.WEEK -> {
                dateEms = 2
                itemDateFormat = SimpleDateFormat("EEE", localeFromContext())
            }
            Grouping.YEAR -> itemDateFormat = Utils.localizedYearLessDateFormat(context)
            Grouping.NONE -> {
                itemDateFormat = Utils.ensureDateFormatWithShortYear(context)
                dateEms = 4
            }
        }
    }

    override fun swapCursor(cursor: Cursor?): Cursor? {
        futureCriterion = if ("current" == prefHandler.getString(
                PrefKey.CRITERION_FUTURE,
                "end_of_day"
            )
        ) System.currentTimeMillis() / 1000 else LocalDate.now().plusDays(1).atStartOfDay().atZone(
            ZoneId.systemDefault()
        ).toEpochSecond()
        return super.swapCursor(cursor)
    }

    fun setAccount(account: Account) {
        mAccount = account
        shouldShowTime =
            UiUtils.getDateMode(account.type, prefHandler) == UiUtils.DateMode.DATE_TIME
        refreshDateFormat()
    }

    internal inner class ViewHolder(view: View) {
        @BindView(R.id.amount)
        lateinit var amount: TextView

        @BindView(R.id.colorAccount)
        lateinit var colorAccount: View

        @BindView(R.id.category)
        lateinit var category: TextView

        @BindView(R.id.color1)
        lateinit var color1: View

        @BindView(R.id.colorContainer)
        lateinit var colorContainer: View

        @BindView(R.id.date)
        lateinit var date: TextView

        @BindView(R.id.voidMarker)
        lateinit var voidMarker: View

        @OnClick(R.id.colorContainer)
        fun toggleCrStatus(v: View) {
            val id = v.tag as Long
            if (id != -1L && onToggleCrStatus != null) {
                onToggleCrStatus.toggle(id)
            }
        }

        init {
            ButterKnife.bind(this, view)
        }
    }

}