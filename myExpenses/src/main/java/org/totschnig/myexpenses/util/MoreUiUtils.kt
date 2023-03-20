package org.totschnig.myexpenses.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils.calculateContrast
import androidx.core.widget.ImageViewCompat
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.filter.ScrollingChip
import org.totschnig.myexpenses.util.UiUtils.DateMode
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun <T> ChipGroup.addChipsBulk(chips: Iterable<T>, closeFunction: ((T) -> Unit)? = null) {
    removeAllViews()
    for (chip in chips) {
        addView(ScrollingChip(context).also { scrollingChip ->
            scrollingChip.text = chip.toString()
            closeFunction?.let {
                scrollingChip.isCloseIconVisible = true
                scrollingChip.setOnCloseIconClickListener {
                    removeView(scrollingChip)
                    it(chip)
                }
            }
        })
    }
}

fun ScrollView.postScrollToBottom() {
    post {
        fullScroll(View.FOCUS_DOWN)
    }
}

fun setNightMode(prefHandler: PrefHandler, context: Context) {
    AppCompatDelegate.setDefaultNightMode(
        when (prefHandler.getString(
            PrefKey.UI_THEME_KEY,
            context.getString(R.string.pref_ui_theme_default)
        )) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}

fun getBestForeground(color: Int) =
    arrayOf(Color.BLACK, Color.WHITE).maxByOrNull { calculateContrast(color, it) }!!

inline fun <reified E : Enum<E>> getEnumFromPreferencesWithDefault(
    prefHandler: PrefHandler,
    prefKey: PrefKey,
    defaultValue: E
) = enumValueOrDefault(prefHandler.getString(prefKey, null), defaultValue)

fun <T : View> findParentWithTypeRecursively(view: View, type: Class<T>): T? {
    if (type.isInstance(view)) {
        @Suppress("UNCHECKED_CAST")
        return view as T
    }
    val parent = view.parent
    return if (parent is View) findParentWithTypeRecursively(parent as View, type) else null
}

fun getDateMode(accountType: AccountType?, prefHandler: PrefHandler) = when {
    (accountType == null || accountType != AccountType.CASH) &&
            prefHandler.getBoolean(PrefKey.TRANSACTION_WITH_VALUE_DATE, false)
    -> DateMode.BOOKING_VALUE
    prefHandler.getBoolean(PrefKey.TRANSACTION_WITH_TIME, true) -> DateMode.DATE_TIME
    else -> DateMode.DATE
}

private fun timeFormatter(accountType: AccountType?, prefHandler: PrefHandler, context: Context) =
    if (getDateMode(accountType, prefHandler) == DateMode.DATE_TIME) {
        android.text.format.DateFormat.getTimeFormat(context) as SimpleDateFormat
    } else null

val SimpleDateFormat.asDateTimeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern(this.toPattern())

fun dateTimeFormatter(account: PageAccount, prefHandler: PrefHandler, context: Context) =
    when (account.grouping) {
        Grouping.DAY -> timeFormatter(account.type, prefHandler, context)?.asDateTimeFormatter
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

fun dateTimeFormatterLegacy(account: PageAccount, prefHandler: PrefHandler, context: Context) =
    when (account.grouping) {
        Grouping.DAY -> {
            timeFormatter(account.type, prefHandler, context)?.let {
                val is24HourFormat = android.text.format.DateFormat.is24HourFormat(context)
                it to if (is24HourFormat) 3f else 4.6f
            }
        }
        Grouping.MONTH ->
            if (prefHandler.getString(PrefKey.GROUP_MONTH_STARTS, "1")!!.toInt() == 1) {
                SimpleDateFormat("dd", Utils.localeFromContext(context)) to 2f
            } else {
                Utils.localizedYearLessDateFormat(context) to 3f
            }
        Grouping.WEEK -> SimpleDateFormat("EEE", Utils.localeFromContext(context)) to 2f
        Grouping.YEAR -> Utils.localizedYearLessDateFormat(context) to 3f
        Grouping.NONE -> Utils.ensureDateFormatWithShortYear(context) to 4.6f
    }

fun Spinner.checkNewAccountLimitation(prefHandler: PrefHandler, context: Context) {
    if (selectedItemId == 0L && !prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true)) {
        (selectedView as? TextView)?.let {
            it.error = ""
            it.setTextColor(Color.RED)
            it.text = ContribFeature.ACCOUNTS_UNLIMITED.buildUsageLimitString(context)
        }
    }
}

fun FloatingActionButton.setBackgroundTintList(color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(getBestForeground(color)))
}

fun View.configurePopupAnchor(
    infoText: CharSequence
) {
    setOnClickListener {
        val host = context.getActivity() ?: throw java.lang.IllegalStateException("BaseActivity expected")
        host.hideKeyboard()
        val infoTextView = LayoutInflater.from(host).inflate(R.layout.textview_info, null) as TextView
        PopupWindow(infoTextView).apply {
            isOutsideTouchable = true
            isFocusable = true
            //without setting background drawable, popup does not close on back button or touch outside, on older API levels
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            infoTextView.text = infoText
            infoTextView.movementMethod = LinkMovementMethod.getInstance()
            showAsDropDown(this@configurePopupAnchor)
        }
    }
}

tailrec fun Context.getActivity(): BaseActivity? = this as? BaseActivity
    ?: (this as? ContextWrapper)?.baseContext?.getActivity()
