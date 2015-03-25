package org.totschnig.myexpenses.test.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 8:32 PM
 */
public class DateTime {

    public static DateTime NULL_DATE = new DateTime(){
        @Override
        public Date asDate() {
            return new Date(0);
        }

        @Override
        public long asLong() {
            return 0;
        }
    };

    private final Calendar c = Calendar.getInstance();

    private DateTime() {}

    public static DateTime today() {
        return new DateTime();
    }

    public static DateTime yesterday() {
        DateTime dt = new DateTime();
        dt.c.add(Calendar.DAY_OF_YEAR, -1);
        return dt;
    }

    public static DateTime date(int year, int month, int day) {
        DateTime dt = new DateTime();
        dt.c.set(Calendar.YEAR, year);
        dt.c.set(Calendar.MONTH, month-1);
        dt.c.set(Calendar.DAY_OF_MONTH, day);
        return dt.atMidnight();
    }

    public static DateTime fromTimestamp(long timestamp) {
        DateTime dt = new DateTime();
        dt.c.setTimeInMillis(timestamp);
        return dt;
    }

    public DateTime atMidnight() {
        return at(0, 0, 0, 0);
    }

    public DateTime atNoon() {
        return at(12, 0, 0, 0);
    }

    public DateTime atDayEnd() {
        return at(23, 59, 59, 999);
    }

    public DateTime at(int hh, int mm, int ss, int ms) {
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, mm);
        c.set(Calendar.SECOND, ss);
        c.set(Calendar.MILLISECOND, ms);
        return this;
    }

    public long asLong() {
        return c.getTimeInMillis();
    }

    public Date asDate() {
        return c.getTime();
    }

}
