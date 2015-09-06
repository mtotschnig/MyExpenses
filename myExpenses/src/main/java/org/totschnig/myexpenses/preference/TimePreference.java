/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.totschnig.myexpenses.R;


/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/17/11 1:59 AM
 */
public class TimePreference extends DialogPreference implements TimePicker.OnTimeChangedListener {

    private static final int DEFAULT_VALUE = 500;

    private int hh;
    private int mm;
    
    public TimePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPersistent(true);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
    }

    @Override
    protected View onCreateDialogView() {
        Context context = getContext();
        TimePicker timePicker = new TimePicker(context);
        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(context));
        timePicker.setOnTimeChangedListener(this);
        timePicker.setCurrentHour(getHour());
        timePicker.setCurrentMinute(getMinute());
        return timePicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) {
            return;
        }
        if (shouldPersist()) {
            persistInt(100*hh+mm);
        }
        notifyChanged();
    }

    private int getHour() {
        return getPersistedInt(DEFAULT_VALUE)/100;
    }

    private int getMinute() {
        int hm = getPersistedInt(DEFAULT_VALUE);
        int h = hm/100;
        return hm-100*h;
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hh, int mm) {
        this.hh = hh;
        this.mm = mm;        
    }

    @Override
    public CharSequence getSummary() {
        return getContext().getString(R.string.pref_auto_backup_time_summary, getHour(), getMinute());
    }
}
