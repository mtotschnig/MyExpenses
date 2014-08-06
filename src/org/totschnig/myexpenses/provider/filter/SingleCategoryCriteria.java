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
public class SingleCategoryCriteria extends IdCriteria {

  public SingleCategoryCriteria(long categoryId, String label) {
    super(MyApplication.getInstance().getString(R.string.category),
        DatabaseConstants.KEY_CATID,
        categoryId,
        label);
  }

  public SingleCategoryCriteria(Parcel in) {
    super(in);
  }

  @Override
  public String getSelection() {
    return  DatabaseConstants.KEY_CATID + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
        + DatabaseConstants.TABLE_CATEGORIES + " WHERE " + DatabaseConstants.KEY_PARENTID + " = ? OR "
        + DatabaseConstants.KEY_ROWID + " = ?)";
  }
  @Override
  public String[] getSelectionArgs() {
    return new String[] {values[0],values[0]};
  }

  @Override
  public String prettyPrint() {
    return prettyPrintInternal(label);
  }
  public static final Parcelable.Creator<SingleCategoryCriteria> CREATOR = new Parcelable.Creator<SingleCategoryCriteria>() {
    public SingleCategoryCriteria createFromParcel(Parcel in) {
        return new SingleCategoryCriteria(in);
    }

    public SingleCategoryCriteria[] newArray(int size) {
        return new SingleCategoryCriteria[size];
    }
  };
  
  public static SingleCategoryCriteria fromStringExtra(String filter) {
    int sepIndex = filter.indexOf(EXTRA_SEPARATOR);
    long id = Long.parseLong(filter.substring(0, sepIndex));
    String label = filter.substring(sepIndex+1);
    return new SingleCategoryCriteria(id, label);
  }
}
