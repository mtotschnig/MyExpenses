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
public class SingleCategoryCriteria extends Criteria {

  private final long categoryId;
  private final String label;

  public SingleCategoryCriteria(long categoryId, String label) {
    super(DatabaseConstants.KEY_CATID, WhereFilter.Operation.EQ, String
        .valueOf(categoryId));
    this.categoryId = categoryId;
    this.label = label;
    this.title = MyApplication.getInstance().getString(R.string.category);
  }

  public SingleCategoryCriteria(Parcel in) {
    super(in);
    categoryId = in.readLong();
    label = in.readString();
  }

  public long getCategoryId() {
    return categoryId;
  }

  @Override
  public String prettyPrint() {
    return prettyPrintInternal(label);
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeLong(categoryId);
    dest.writeString(label);
  }
  public static final Parcelable.Creator<SingleCategoryCriteria> CREATOR = new Parcelable.Creator<SingleCategoryCriteria>() {
    public SingleCategoryCriteria createFromParcel(Parcel in) {
        return new SingleCategoryCriteria(in);
    }

    public SingleCategoryCriteria[] newArray(int size) {
        return new SingleCategoryCriteria[size];
    }
};
}
