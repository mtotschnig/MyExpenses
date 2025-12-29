/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.provider

import android.content.Context
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.MyApplication.Companion.instance
import org.totschnig.myexpenses.db2.localizedLabelForPaymentMethod
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.preference.PrefHandler
import java.util.Calendar
import java.util.Locale

object DatabaseConstants {
    private var isLocalized = false
    @JvmField
    var weekStartsOn: Int = 0
    var monthStartsOn: Int = 0
   lateinit var YEAR_OF_WEEK_START: String
   lateinit var YEAR_OF_MONTH_START: String
   lateinit var WEEK: String
   lateinit var MONTH: String
   lateinit var THIS_YEAR_OF_WEEK_START: String
   lateinit var THIS_YEAR_OF_MONTH_START: String
   lateinit var THIS_WEEK: String
   lateinit var THIS_MONTH: String
   lateinit var WEEK_START: String
   lateinit var COUNT_FROM_WEEK_START_ZERO: String
   lateinit var WEEK_START_JULIAN: String
    lateinit var WEEK_MAX: String

    //in sqlite julian days are calculated from noon, in order to make sure that the returned julian day matches the day we need, we set the time to noon.
    private const val JULIAN_DAY_OFFSET = "'start of day','+12 hours'"

    private lateinit var PROJECTION_BASE: Array<String>
    private lateinit var PROJECTION_EXTENDED: Array<String>

    @JvmStatic
    fun buildLocalized(locale: Locale, myApplication: MyApplication) {
        val appComponent = myApplication.appComponent
        buildLocalized(
            locale,
            myApplication,
            appComponent.prefHandler()
        )
    }

    fun buildLocalized(locale: Locale, context: Context, prefHandler: PrefHandler) {
        weekStartsOn = prefHandler.weekStartWithFallback(locale)
        monthStartsOn = prefHandler.monthStart
        val monthDelta = monthStartsOn - 1
        val nextWeekEndSqlite: Int
        val nextWeekStartsSqlite = weekStartsOn - 1 //Sqlite starts with Sunday = 0
        if (weekStartsOn == Calendar.SUNDAY) {
            //weekStartsOn Sunday
            nextWeekEndSqlite = 6
        } else {
            //weekStartsOn Monday or Saturday
            nextWeekEndSqlite = weekStartsOn - 2
        }
        YEAR_OF_WEEK_START =
            "CAST(strftime('%Y',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)"
        YEAR_OF_MONTH_START =
            "CAST(strftime('%Y',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer)"
        WEEK_START =
            "date(date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day')"
        THIS_YEAR_OF_WEEK_START =
            "CAST(strftime('%Y','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') AS integer)"
        WEEK =
            "CAST((strftime('%j',date,'unixepoch','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)" //calculated for the beginning of the week
        MONTH =
            "CAST(strftime('%m',date,'unixepoch','localtime','-" + monthDelta + " day') AS integer) - 1" //convert to 0 based
        THIS_WEEK =
            "CAST((strftime('%j','now','localtime','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)"
        THIS_MONTH =
            "CAST(strftime('%m','now','localtime','-" + monthDelta + " day') AS integer) - 1"
        THIS_YEAR_OF_MONTH_START =
            "CAST(strftime('%Y','now','localtime','-" + monthDelta + " day') AS integer)"
        COUNT_FROM_WEEK_START_ZERO = "date('%d-01-01','weekday " + nextWeekStartsSqlite + "', '" +
                "-7 day" +
                "' ,'+%d day')"
        WEEK_START_JULIAN =
            "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ",'weekday " + nextWeekEndSqlite + "', '-6 day')"
        WEEK_MAX =
            "CAST((strftime('%%j','%d-12-31','weekday " + nextWeekEndSqlite + "', '-6 day') - 1) / 7 + 1 AS integer)"
        buildProjection(context)
        isLocalized = true
    }

    fun buildProjection(context: Context) {
        PROJECTION_BASE = arrayOf(
            KEY_ROWID,
            KEY_ACCOUNTID,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT + " AS " + KEY_DISPLAY_AMOUNT,
            KEY_COMMENT,
            KEY_CATID,
            KEY_PATH,
            KEY_PAYEEID,
            KEY_PAYEE_NAME,
            KEY_TRANSFER_PEER,
            KEY_TRANSFER_ACCOUNT,
            TRANSFER_ACCOUNT_LABEL,
            KEY_METHODID,
            localizedLabelForPaymentMethod(context, KEY_METHOD_LABEL) + " AS " + KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            "$YEAR_OF_WEEK_START AS $KEY_YEAR_OF_WEEK_START",
            "$YEAR_OF_MONTH_START AS $KEY_YEAR_OF_MONTH_START",
            "$YEAR AS $KEY_YEAR",
            "$MONTH AS $KEY_MONTH",
            "$WEEK AS $KEY_WEEK",
            "$DAY AS $KEY_DAY",
            "$THIS_YEAR_OF_WEEK_START AS $KEY_THIS_YEAR_OF_WEEK_START",
            "$THIS_YEAR_OF_MONTH_START AS $KEY_THIS_YEAR_OF_MONTH_START",
            "$THIS_YEAR AS $KEY_THIS_YEAR",
            "$THIS_WEEK AS $KEY_THIS_WEEK",
            "$THIS_DAY AS $KEY_THIS_DAY",
            "$WEEK_START AS $KEY_WEEK_START"
        )

        //extended
        PROJECTION_EXTENDED = arrayOf(
            *PROJECTION_BASE,
            KEY_COLOR,
            KEY_TRANSFER_PEER_IS_PART,
            KEY_STATUS,KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            KEY_TAGLIST,
            KEY_PARENTID
        )
    }


    private fun ensureLocalized() {
        if (!isLocalized) {
            buildLocalized(Locale.getDefault(), instance)
        }
    }

    //if we do not cast the result to integer, we would need to do the conversion in Java
    const val YEAR: String = "CAST(strftime('%Y',date,'unixepoch','localtime') AS integer)"
    const val MONTH_PLAIN: String =
        "CAST(strftime('%m',date,'unixepoch','localtime') AS integer) - 1" //convert to 0 based
    const val THIS_DAY: String = "CAST(strftime('%j','now','localtime') AS integer)"
    const val DAY: String = "CAST(strftime('%j',date,'unixepoch','localtime') AS integer)"
    const val THIS_YEAR: String = "CAST(strftime('%Y','now','localtime') AS integer)"
    val DAY_START_JULIAN: String =
        "julianday(date,'unixepoch','localtime'," + JULIAN_DAY_OFFSET + ")"

    const val TREE_CATEGORIES: String = "Tree"

    val CAT_AS_LABEL: String = fullCatCase(null) + " AS " + KEY_LABEL

    val TRANSFER_ACCOUNT_UUID: String =
        "(SELECT " + KEY_UUID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ") AS " + KEY_TRANSFER_ACCOUNT

    val TRANSFER_CURRENCY: String = String.format(
        "(select %1\$s from %2\$s where %3\$s=%4\$s) AS %5\$s",
        KEY_CURRENCY,
        TABLE_ACCOUNTS,
        KEY_ROWID,
        KEY_TRANSFER_ACCOUNT,
        KEY_TRANSFER_CURRENCY
    )

    val CATEGORY_ICON: String = "CASE WHEN " +
            "  " + KEY_CATID + " " +
            " THEN " +
            "  (SELECT " + KEY_ICON + " FROM " + TABLE_CATEGORIES + " WHERE " + KEY_ROWID + " = " + KEY_CATID + ") " +
            " ELSE null" +
            " END AS " + KEY_ICON


    @JvmField
    val WHERE_NOT_SPLIT: String = KEY_CATID + " IS NOT " + SPLIT_CATID
    val WHERE_NOT_SPLIT_PART: String = KEY_PARENTID + " IS null"
    val WHERE_NOT_VOID: String = KEY_CR_STATUS + " != '" + CrStatus.VOID.name + "'"
    val WHERE_NOT_ARCHIVED: String = KEY_STATUS + " != " + STATUS_ARCHIVED
    val WHERE_NOT_ARCHIVE: String = KEY_STATUS + " != " + STATUS_ARCHIVE

    @JvmField
    val WHERE_DEPENDENT: String = (KEY_PARENTID + " = ? OR " + KEY_ROWID + " IN "
            + "(SELECT " + KEY_TRANSFER_PEER + " FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_PARENTID + "= ?)")

    @JvmField
    val WHERE_SELF_OR_PEER: String = KEY_TRANSFER_PEER + " = ? OR " + KEY_ROWID + " = ?"

    @JvmField
    val WHERE_SELF_OR_RELATED: String = WHERE_SELF_OR_PEER + " OR " + WHERE_DEPENDENT

    val IS_SAME_CURRENCY: String = KEY_CURRENCY + " = (SELECT " + KEY_CURRENCY + " from " +
            TABLE_ACCOUNTS + " WHERE " + KEY_ROWID + " = " + KEY_TRANSFER_ACCOUNT + ")"

    @JvmStatic
    val yearOfWeekStart: String
        get() {
            ensureLocalized()
            return YEAR_OF_WEEK_START
        }

    val yearOfMonthStart: String
        get() {
            ensureLocalized()
            return YEAR_OF_MONTH_START
        }

    @JvmStatic
    val week: String
        get() {
            ensureLocalized()
            return WEEK
        }

    val month: String
        get() {
            ensureLocalized()
            return MONTH
        }

    val thisYearOfWeekStart: String
        get() {
            ensureLocalized()
            return THIS_YEAR_OF_WEEK_START
        }


    val thisYearOfMonthStart: String
        get() {
            ensureLocalized()
            return THIS_YEAR_OF_MONTH_START
        }

    val weekStartJulian: String
        get() {
            ensureLocalized()
            return WEEK_START_JULIAN
        }

    val thisWeek: String
        get() {
            ensureLocalized()
            return THIS_WEEK
        }

    val thisMonth: String
        get() {
            ensureLocalized()
            return THIS_MONTH
        }

    @JvmStatic
    val weekStart: String
        get() {
            ensureLocalized()
            return WEEK_START
        }

    val countFromWeekStartZero: String
        /**
         * we want to find out the week range when we are given a week number
         * we find out the first day in the year, that is the firstdayofweek of the locale and is
         * one week behind the first day with week number 1
         * add (weekNumber)*7 days to get at the beginning of the week
         */
        get() {
            ensureLocalized()
            return COUNT_FROM_WEEK_START_ZERO
        }

    val weekMax: String
        get() {
            ensureLocalized()
            return WEEK_MAX
        }

    @JvmStatic
    val projectionBase: Array<String>
        get() {
            ensureLocalized()
            return PROJECTION_BASE
        }

    @JvmStatic
    val projectionExtended: Array<String>
        get() {
            ensureLocalized()
            return PROJECTION_EXTENDED
        }
}
