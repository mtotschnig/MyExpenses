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
import android.support.v7.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.totschnig.myexpenses.R;


/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/17/11 1:59 AM
 * migrated to Support Library prefereence-v7 by Michael Totschnig
 */
public class TimePreference extends IntegerDialogPreference {

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
        return getContext().getString(R.string.pref_auto_backup_time_summary, getHour(), getMinute());
    }

}
