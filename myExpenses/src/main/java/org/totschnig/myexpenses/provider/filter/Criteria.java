/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Based on Financisto (c) 2010 Denis Solonenko, made available
 *   under the terms of the GNU Public License v2.0
 */

package org.totschnig.myexpenses.provider.filter;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

public abstract class Criteria implements Parcelable {

  protected static final String EXTRA_SEPARATOR = ";";
  protected static final String EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP = "(?<!\\\\)\\;";

  public static String escapeSeparator(String in) {
    return in.replace(";", "\\;");
  }

  public static String unescapeSeparator(String in) {
    return in.replace("\\;", ";");
  }

  public final WhereFilter.Operation operation;
  public final String[] values;

  public abstract int getID();

  abstract String getColumn();

  public Criteria(Operation operation,
                  String... values) {
    this.operation = operation;
    this.values = values;
  }

  Criteria(CriteriaInfo c) {
    this.operation = c.getOperation();
    this.values = c.getValues();
  }

  public Criteria(Parcel in) {
    operation = Operation.valueOf(in.readString());
    values = in.createStringArray();
  }

  public boolean isNull() {
    return operation == WhereFilter.Operation.ISNULL;
  }

  public String getSelection() {
    return getColumn() + " " + operation.getOp(values.length);
  }

  public int size() {
    return values != null ? values.length : 0;
  }

  public String[] getSelectionArgs() {
    return values;
  }

  public String prettyPrint(Context context) {
    return TextUtils.join(", ", values);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(operation.name());
    dest.writeStringArray(values);
  }

  public String toStringExtra() {
    throw new UnsupportedOperationException("Only subclasses can be persisted");
  }

  /**
   * @param selection
   * @return selection wrapped in a way that it also finds split transactions with parts
   * that are matched by the critera
   */
  protected String applyToSplitParts(String selection, String tableName) {
    if (!shouldApplyToParts()) {
      return selection;
    }
    return "(" + selection + " OR (" + KEY_CATID + " = " + DatabaseConstants.SPLIT_CATID //maybe the check for split catId is not needed
        + " AND exists(select 1 from " + TABLE_TRANSACTIONS + " children"
        + " WHERE " + KEY_PARENTID
        + " = " + tableName + "." + KEY_ROWID + " AND (" + selection + "))))";
  }

  /**
   * the sums are calculated based on split parts, hence here we must take care to select parts
   * where the parents match
   *
   * @param selection
   * @return selection wrapped in a way that is also finds split parts where parents are
   * matched by the criteria
   */
  protected String applyToSplitParents(String selection, String tableName) {
    String selectParents;
    if (!shouldApplyToParts()) {
      selectParents = "(" + selection + " AND " + KEY_PARENTID + " IS NULL)";
    } else {
      selectParents = selection;
    }
    return "(" + selectParents + " OR  exists(select 1 from " + TABLE_TRANSACTIONS + " parents"
        + " WHERE " + KEY_ROWID
        + " = " + tableName + "." + KEY_PARENTID + " AND (" + selection + ")))";
  }

  public String getSelectionForParts(String tableName) {
    return applyToSplitParents(getSelection(), tableName);
  }

  public String getSelectionForParents(String tableName) {
    return applyToSplitParts(getSelection(), tableName);
  }

  protected boolean shouldApplyToParts() {
    return true;
  }
}
 