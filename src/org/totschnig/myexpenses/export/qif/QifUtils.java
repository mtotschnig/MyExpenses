/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import android.text.TextUtils;
import android.util.Log;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import static org.totschnig.myexpenses.export.qif.QifDateFormat.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/12/11 11:40 PM
 */
public class QifUtils {

    private static final Pattern DATE_DELIMITER_PATTERN = Pattern.compile("/|'|\\.|-");
    private static final Pattern MONEY_PREFIX_PATTERN = Pattern.compile("\\D");
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    public static String trimFirstChar(String s) {
        return s.length() > 1 ? s.substring(1) : "";
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
     * @param sDate String QIF date to parse
     * @param format String identifier of format to parse
     * @return Returns parsed date and current date if an error occurs
     */
    public static Date parseDate(String sDate, QifDateFormat format) {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);

        String[] chunks = DATE_DELIMITER_PATTERN.split(sDate);

        if (format == US) {
            try {
                month = Integer.parseInt(chunks[0].trim());
                day = Integer.parseInt(chunks[1].trim());
                year = Integer.parseInt(chunks[2].trim());
            } catch (Exception e) {
                //eat it
                Log.e("QifUtils", "Unable to parse US date", e);
            }
        } else if (format == EU) {
            try {
                day = Integer.parseInt(chunks[0].trim());
                month = Integer.parseInt(chunks[1].trim());
                year = Integer.parseInt(chunks[2].trim());
            } catch (Exception e) {
                Log.e("QifUtils", "Unable to parse EU date", e);
            }
        } else if (format == YMD) {
          try {
            year = Integer.parseInt(chunks[0].trim());
            month = Integer.parseInt(chunks[1].trim());
            day = Integer.parseInt(chunks[2].trim());
          } catch (Exception e) {
              Log.e("QifUtils", "Unable to parse YMD date", e);
          }
        }  else {
            Log.e("QifUtils", "Invalid date format specified");
            return new Date();
        }

        if (year < 100) {
            if (year < 29) {
                year += 2000;
            } else {
                year += 1900;
            }
        }
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Adopted from http://jgnash.svn.sourceforge.net/viewvc/jgnash/jgnash2/trunk/src/jgnash/imports/qif/QifUtils.java
     */
    public static long parseMoney(String money) {
        String sMoney = money;

        if (sMoney != null) {
            BigDecimal bdMoney;
            sMoney = sMoney.trim(); // to be safe
            try {
                bdMoney = new BigDecimal(sMoney);
                return moneyAsLong(bdMoney);
            } catch (NumberFormatException e) {
                /* there must be commas, etc in the number.  Need to look for them
                 * and remove them first, and then try BigDecimal again.  If that
                 * fails, then give up and use NumberFormat and scale it down
                 * */
                String[] split = MONEY_PREFIX_PATTERN.split(sMoney);
                if (split.length > 2) {
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
                        bdMoney = new BigDecimal(buf.toString());
                        return moneyAsLong(bdMoney);
                    } catch (final NumberFormatException e2) {
                        Log.e("QifUtils", "Second parse attempt failed, falling back to rounding");
                    }
                }
                NumberFormat formatter = NumberFormat.getNumberInstance();
                try {
                    Number num = formatter.parse(sMoney);
                    BigDecimal bd = new BigDecimal(num.floatValue());
                    if (bd.scale() > 6) {
                        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                    return moneyAsLong(bd);
                } catch (ParseException ignored) {
                }
                Log.e("QifUtils", "Could not parse money " + sMoney);
            }
        }
        return 0;
    }

    private static long moneyAsLong(BigDecimal bd) {
        return bd.multiply(HUNDRED).intValue();
    }

    public static boolean isTransferCategory(String category) {
        return !TextUtils.isEmpty(category) && category.startsWith("[") && category.endsWith("]");
    }

}
