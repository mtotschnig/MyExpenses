/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.util.AttributeSet;

import org.totschnig.myexpenses.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/17/11 1:59 AM
 * migrated to Support Library prefereence-v7 by Michael Totschnig
 */
public class TimePreference extends IntegerDialogPreference {

    public static final int DEFAULT_VALUE = 700;

    public TimePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.timepicker);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.timepicker);
    }

    public int getHour() {
        return getPersistedInt(DEFAULT_VALUE)/100;
    }

    public int getMinute() {
        int hm = getPersistedInt(DEFAULT_VALUE);
        int h = hm/100;
        return hm-100*h;
    }

    @Override
    public CharSequence getSummary() {
        return String.format(Locale.getDefault(),"%1$02d:%2$02d",getHour(), getMinute());
    }

    public static Date getScheduledTime(PrefHandler prefHandler, PrefKey prefKey) {
        int hhmm = prefHandler.getInt(prefKey, DEFAULT_VALUE);
        int hh = hhmm / 100;
        int mm = hhmm - 100 * hh;
        Calendar c = Calendar.getInstance();
        long now = System.currentTimeMillis();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, mm);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() < now) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return c.getTime();
    }
}
