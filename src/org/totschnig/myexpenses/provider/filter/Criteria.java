/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.provider.filter;

import android.content.Intent;
import android.text.TextUtils;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.orb.Expression;
import org.totschnig.myexpenses.provider.orb.Expressions;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class Criteria {

  public static Criteria eq(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.EQ, value);
  }

  public static Criteria neq(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.NEQ, value);
  }

  public static Criteria btw(String column, String value1, String value2) {
    return new Criteria(column, WhereFilter.Operation.BTW, value1, value2);
  }

  public static Criteria gt(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.GT, value);
  }

  public static Criteria gte(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.GTE, value);
  }

  public static Criteria lt(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.LT, value);
  }

  public static Criteria lte(String column, String value) {
    return new Criteria(column, WhereFilter.Operation.LTE, value);
  }

  public static Criteria isNull(String column) {
    return new Criteria(column, WhereFilter.Operation.ISNULL);
  }

  public static Criteria raw(String text) {
    return new Criteria("(" + text + ")", WhereFilter.Operation.NOPE);
  }

  public final String columnName;
  public final WhereFilter.Operation operation;
  public final String[] values;

  public Criteria(String columnName, WhereFilter.Operation operation,
      String... values) {
    this.columnName = columnName;
    this.operation = operation;
    this.values = values;
  }

  public boolean isNull() {
    return operation == WhereFilter.Operation.ISNULL;
  }

  public Expression toWhereExpression() {
    switch (operation) {
    case EQ:
      return Expressions.eq(columnName, getLongValue1());
    case GT:
      return Expressions.gt(columnName, getLongValue1());
    case GTE:
      return Expressions.gte(columnName, getLongValue1());
    case LT:
      return Expressions.lt(columnName, getLongValue1());
    case LTE:
      return Expressions.lte(columnName, getLongValue1());
    case BTW:
      return Expressions.btw(columnName, getLongValue1(), getLongValue2());
    }
    throw new IllegalArgumentException();
  }

  public String toStringExtra() {
    StringBuilder sb = new StringBuilder();
    sb.append(columnName).append(",");
    sb.append(operation.name()).append(",");
    String[] values = this.values;
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(values[i]);
    }
    return sb.toString();
  }

  public static Criteria fromStringExtra(String extra) {
    String[] a = extra.split(",");
    if (DatabaseConstants.KEY_CATID.equals(a[0])) {
      return SingleCategoryCriteria.fromStringExtra(extra);
    } else {
      String[] values = new String[a.length - 2];
      System.arraycopy(a, 2, values, 0, values.length);
      return new Criteria(a[0], WhereFilter.Operation.valueOf(a[1]), values);
    }
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

  public void toIntent(String title, Intent intent) {
    intent.putExtra(WhereFilter.TITLE_EXTRA, title);
    intent.putExtra(WhereFilter.FILTER_EXTRA, new String[] { toStringExtra() });
  }

  public String prettyPrint() {
    return TextUtils.join(", ", values);
  }
}
