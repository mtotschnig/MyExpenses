/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.totschnig.myexpenses.R;


/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/17/11 1:59 AM
 */
public class TimePreference extends DialogPreference  {

    public static final int DEFAULT_VALUE = 500;

    private TimePicker mTimePicker;
    
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
        mTimePicker = new TimePicker(context);
        mTimePicker.setId(1);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(context));
        mTimePicker.setCurrentHour(getHour());
        mTimePicker.setCurrentMinute(getMinute());
        return mTimePicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }
        if (shouldPersist()) {
            mTimePicker.clearFocus();
            persistInt(100*mTimePicker.getCurrentHour()+mTimePicker.getCurrentMinute());
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
    public CharSequence getSummary() {
        return getContext().getString(R.string.pref_auto_backup_time_summary, getHour(), getMinute());
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
    }
}
