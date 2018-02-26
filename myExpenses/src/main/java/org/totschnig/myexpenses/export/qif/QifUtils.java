/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.totschnig.myexpenses.model.Money;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.regex.Pattern;

import timber.log.Timber;

import static org.totschnig.myexpenses.export.qif.QifDateFormat.EU;
import static org.totschnig.myexpenses.export.qif.QifDateFormat.US;
import static org.totschnig.myexpenses.export.qif.QifDateFormat.YMD;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/12/11 11:40 PM
 */
public class QifUtils {

    private static final Pattern DATE_DELIMITER_PATTERN = Pattern.compile("/|'|\\.|-");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern HOUR_DELIMITER_PATTERN = Pattern.compile(":");
    private static final Pattern MONEY_PREFIX_PATTERN = Pattern.compile("\\D");
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    private QifUtils() {
    }

    public static String trimFirstChar(String s) {
        return s.length() > 1 ? s.substring(1) : "";
    }

    /**
     * First tries to parse input as a date in the specified format. If this fails, try to split
     * input on white space in two chunks, and tries to parse first chunk as date, second as time
     * @param sDateTime
     * @param format
     * @return
     */
    public static Date parseDate(String sDateTime, QifDateFormat format) {
        try {
            return parseDateInternal(sDateTime, format).getTime();
        }  catch (NumberFormatException | IndexOutOfBoundsException e) {
            String[] dateTimeChunks = WHITESPACE_PATTERN.split(sDateTime);
            if (dateTimeChunks.length > 1) {
                try {
                    Calendar cal = parseDateInternal(dateTimeChunks[0], format);
                    String[] timeChunks = HOUR_DELIMITER_PATTERN.split(dateTimeChunks[1]);
                    cal.set(Calendar.HOUR_OF_DAY, parseInt(timeChunks,0, 0));
                    cal.set(Calendar.MINUTE,  parseInt(timeChunks, 1, 0));
                    cal.set(Calendar.SECOND,  parseInt(timeChunks, 2, 0));
                    return cal.getTime();
                } catch (NumberFormatException | IndexOutOfBoundsException ignored) {}
            }
            return new Date();
        }
    }

    /**
     * Adopted from http://jgnash.svn.sourceforge.net/viewvc/jgnash/jgnash2/trunk/src/jgnash/imports/qif/QifUtils.java
     *
     * Converts a string into a data object
     * <p>
     * <p/>
     * format "6/21' 1" -> 6/21/2001 format "6/21'01" -> 6/21/2001 format "9/18'2001 -> 9/18/2001 format "06/21/2001"
     * format "06/21/01" format "3.26.03" -> German version of quicken format "03-26-2005" -> MSMoney format format
     * "1.1.2005" -> kmymoney2 20.1.94 European dd/mm/yyyy has been confirmed
     * <p/>
     * 21/2/07 -> 02/21/2007 UK, Quicken 2007 D15/2/07
     *
     * @param sDateTime String QIF date to parse
     * @param format String identifier of format to parse
     * @return Returns parsed date and current date if an error occurs
     */
    public static Calendar parseDateInternal(String sDateTime, QifDateFormat format) {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);
        int hourOfDay = 0;
        int minute = 0;
        int second = 0;

        String[] dateChunks = DATE_DELIMITER_PATTERN.split(sDateTime);

        if (format == US) {
            month = parseInt(dateChunks, 0);
            day = parseInt(dateChunks, 1);
            year = parseInt(dateChunks, 2);
        } else if (format == EU) {
            day = parseInt(dateChunks, 0);
            month = parseInt(dateChunks, 1);
            year = parseInt(dateChunks, 2);
        } else if (format == YMD) {
            year = parseInt(dateChunks, 0);
            month = parseInt(dateChunks, 1);
            day = parseInt(dateChunks, 2);
        }

        if (year < 100) {
            if (year < 29) {
                year += 2000;
            } else {
                year += 1900;
            }
        }
        cal.set(year, month - 1, day, hourOfDay, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static int parseInt(String[] array, int position, int defaultValue) {
        try {
            return parseInt(array, position);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return defaultValue;
        }
    }

    private static int parseInt(String[] array, int position) {
        return Integer.parseInt(array[position].trim());
    }

    public static BigDecimal parseMoney(@NonNull String money) {
        return parseMoney(money, Integer.MAX_VALUE);
    }

  /**
   * Parse the string with a maxSize, that takes the number of fraction digits of currency into account,
   * so that the number representing the amount in the database does not exceed approximately 1/10th of {@link Long#MAX_VALUE}
   * @param money
   * @param currency
   * @return
   */
    public static BigDecimal parseMoney(@NonNull String money, Currency currency) {
        return parseMoney(money, 18 - Money.getFractionDigits(currency));
    }

    /**
     * Adopted from http://jgnash.svn.sourceforge.net/viewvc/jgnash/jgnash2/trunk/src/jgnash/imports/qif/QifUtils.java
     * @param money String to be parsed
     * @param maxSize maxSize of the result, as calculated by precision - scale, i.e.  the number of digits in front of the decimal point
     * @return BigDecimal, if the result exceeds maxSize, {@link IllegalArgumentException} is thrown
     */
    public static BigDecimal parseMoney(@NonNull String money, int maxSize) {
        BigDecimal result;

        String sMoney = money.trim().replace(" ",""); // to be safe
        try {
            result = new BigDecimal(sMoney);
        } catch (NumberFormatException e) {
            /* there must be commas, etc in the number.  Need to look for them
             * and remove them first, and then try BigDecimal again.  If that
             * fails, then give up and use NumberFormat and scale it down
             * */
            String[] split = MONEY_PREFIX_PATTERN.split(sMoney);
            if (split.length >= 2) {
                StringBuilder buf = new StringBuilder();
                if (sMoney.startsWith("-")) {
                    buf.append('-');
                }
                for (int i = 0; i < split.length - 1; i++) {
                    buf.append(split[i]);
                }
                buf.append('.');
                buf.append(split[split.length - 1]);

                try {
                    result = new BigDecimal(buf.toString());
                } catch (final NumberFormatException e2) {
                    NumberFormat formatter = NumberFormat.getNumberInstance();
                    try {
                        Number num = formatter.parse(sMoney);
                        result = new BigDecimal(num.floatValue());
                    } catch (ParseException ignored) {
                        result = new BigDecimal(0);
                    }
                    Timber.e("Could not parse money %s", sMoney);
                }
            } else {
                result = new BigDecimal(0);
            }
        }
        if (result.precision() - result.scale() > maxSize) {
            throw new IllegalArgumentException(result.toString() + " exceeds maximum size of " + maxSize);
        }
        return result;
    }

    public static boolean isTransferCategory(String category) {
        return !TextUtils.isEmpty(category) && category.startsWith("[") && category.endsWith("]");
    }

    public static boolean twoSidesOfTheSameTransfer(QifAccount fromAccount,
                                                    QifTransaction fromTransaction, QifAccount toAccount,
                                                    QifTransaction toTransaction) {
        return toTransaction.isTransfer()
            && toTransaction.toAccount.equals(fromAccount.memo)
            && fromTransaction.toAccount.equals(toAccount.memo)
            && fromTransaction.date.equals(toTransaction.date)
            && fromTransaction.amount.equals(toTransaction.amount.negate());
    }
}
