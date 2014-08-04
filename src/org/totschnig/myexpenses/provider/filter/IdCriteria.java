/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.provider.filter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class IdCriteria extends Criteria {

  protected final String label;

  public IdCriteria(String title, String column, long id, String label) {
    super(column, WhereFilter.Operation.EQ, String
        .valueOf(id));
    this.label = label;
    this.title = title;
  }

  public IdCriteria(Parcel in) {
    super(in);
    label = in.readString();
  }

  @Override
  public String prettyPrint() {
    return prettyPrintInternal(label);
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(label);
  }
  public static final Parcelable.Creator<IdCriteria> CREATOR = new Parcelable.Creator<IdCriteria>() {
    public IdCriteria createFromParcel(Parcel in) {
        return new IdCriteria(in);
    }

    public IdCriteria[] newArray(int size) {
        return new IdCriteria[size];
    }
};
}
