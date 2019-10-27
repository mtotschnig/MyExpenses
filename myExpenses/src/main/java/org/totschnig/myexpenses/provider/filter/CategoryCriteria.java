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

import android.os.Parcel;
import android.os.Parcelable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;

public class CategoryCriteria extends IdCriteria {
  static final String COLUMN = KEY_CATID;

  public CategoryCriteria(String label, long... ids) {
    super(label, ids);
  }

  @SuppressWarnings("unused")
  public CategoryCriteria(String label, String... ids) {
    super(label, ids);
  }

  @Override
  public int getID() {
    return R.id.FILTER_CATEGORY_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  private CategoryCriteria(Parcel in) {
    super(in);
  }

  public CategoryCriteria() {
    super();
  }

  @Override
  public String getSelection() {
    if (operation == WhereFilter.Operation.ISNULL) {
      return super.getSelection();
    }
    String selection = WhereFilter.Operation.IN.getOp(values.length);
    return getColumn() + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
        + TABLE_CATEGORIES + " WHERE " + KEY_PARENTID + " " + selection + " OR "
        + KEY_ROWID + " " + selection + ")";
  }

  @Override
  public String[] getSelectionArgs() {
    return Utils.joinArrays(values, values);
  }

  public static final Parcelable.Creator<CategoryCriteria> CREATOR = new Parcelable.Creator<CategoryCriteria>() {
    public CategoryCriteria createFromParcel(Parcel in) {
      return new CategoryCriteria(in);
    }

    public CategoryCriteria[] newArray(int size) {
      return new CategoryCriteria[size];
    }
  };

  public static Criteria fromStringExtra(String extra) {
    return extra.equals("null") ? new CategoryCriteria() : IdCriteria.fromStringExtra(extra, CategoryCriteria.class);
  }
}
