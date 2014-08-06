/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.provider.filter;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class MethodCriteria extends IdCriteria {

  public MethodCriteria(long id, String label) {
    super(MyApplication.getInstance().getString(R.string.method),
        DatabaseConstants.KEY_METHODID, id, label);
  }

  public MethodCriteria(Parcel in) {
    super(in);
  }

  public static final Parcelable.Creator<MethodCriteria> CREATOR = new Parcelable.Creator<MethodCriteria>() {
    public MethodCriteria createFromParcel(Parcel in) {
        return new MethodCriteria(in);
    }

    public MethodCriteria[] newArray(int size) {
        return new MethodCriteria[size];
    }
  };
  public static MethodCriteria fromStringExtra(String extra) {
    int sepIndex = extra.indexOf(EXTRA_SEPARATOR);
    long id = Long.parseLong(extra.substring(0, sepIndex));
    String label = extra.substring(sepIndex+1);
    return new MethodCriteria(id,label);
  }
}
