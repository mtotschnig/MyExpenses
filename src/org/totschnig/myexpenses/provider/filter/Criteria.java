/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.provider.filter;


import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
@SuppressLint("ParcelCreator")
public class Criteria implements Parcelable {

  public String title;
  public final String columnName;
  public final WhereFilter.Operation operation;
  public final String[] values;

  public Criteria(String columnName, WhereFilter.Operation operation,
      String... values) {
    this.columnName = columnName;
    this.operation = operation;
    this.values = values;
  }
  

  public Criteria(Criteria c) {
    this.columnName = c.columnName;
    this.operation = c.operation;
    this.values = c.values;
  }

  public Criteria(Parcel in) {
    title = in.readString();
    columnName = in.readString();
    operation = Operation.valueOf(in.readString());
    values = in.createStringArray();
  }

  public boolean isNull() {
    return operation == WhereFilter.Operation.ISNULL;
  }

  public String getStringValue() {
    return values[0];
  }

  public int getIntValue() {
    return Integer.parseInt(values[0]);
  }

  public long getLongValue1() {
    return Long.parseLong(values[0]);
  }

  public long getLongValue2() {
    return Long.parseLong(values[1]);
  }

  public String getSelection() {
    return columnName + " " + operation.op;
  }

  public int size() {
    return values != null ? values.length : 0;
  }

  public String[] getSelectionArgs() {
    return values;
  }

  public String prettyPrint() {
    return TextUtils.join(", ", values);
  }

  @Override
  public int describeContents() {
    return 0;
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(title);
    dest.writeString(columnName);
    dest.writeString(operation.name());
    dest.writeStringArray(values);
  }
  protected String prettyPrintInternal(String value) {
    return title + " : " + value;
  }
}
