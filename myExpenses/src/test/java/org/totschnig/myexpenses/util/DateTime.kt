package org.totschnig.myexpenses.util

import java.util.*

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 8:32 PM
 */
open class DateTime private constructor() {
    private val c = Calendar.getInstance()
    fun atMidnight(): DateTime {
        return at(0, 0, 0, 0)
    }

    fun atNoon(): DateTime {
        return at(12, 0, 0, 0)
    }

    fun atDayEnd(): DateTime {
        return at(23, 59, 59, 999)
    }

    fun at(hh: Int, mm: Int, ss: Int, ms: Int): DateTime {
        c[Calendar.HOUR_OF_DAY] = hh
        c[Calendar.MINUTE] = mm
        c[Calendar.SECOND] = ss
        c[Calendar.MILLISECOND] = ms
        return this
    }

    open fun asLong(): Long {
        return c.timeInMillis
    }

    open fun asDate(): Date {
        return c.time
    }

    companion object {
        val NULL_DATE: DateTime = object : DateTime() {
            override fun asDate(): Date {
                return Date(0)
            }

            override fun asLong(): Long {
                return 0
            }
        }

        fun today(): DateTime {
            return DateTime()
        }

        fun yesterday(): DateTime {
            val dt = DateTime()
            dt.c.add(Calendar.DAY_OF_YEAR, -1)
            return dt
        }

        fun date(year: Int, month: Int, day: Int): DateTime {
            val dt = DateTime()
            dt.c[Calendar.YEAR] = year
            dt.c[Calendar.MONTH] = month - 1
            dt.c[Calendar.DAY_OF_MONTH] = day
            return dt.atMidnight()
        }

        fun fromTimestamp(timestamp: Long): DateTime {
            val dt = DateTime()
            dt.c.timeInMillis = timestamp
            return dt
        }
    }
}