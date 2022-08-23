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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DbConstantsKt;

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
    return getColumn() + " IN (" + DbConstantsKt.categoryTreeSelect(null, null, new String[] { KEY_ROWID }, null, WhereFilter.Operation.IN.getOp(values.length), null)  + ")";
  }

  public static final Parcelable.Creator<CategoryCriteria> CREATOR = new Parcelable.Creator<CategoryCriteria>() {
    public CategoryCriteria createFromParcel(Parcel in) {
      return new CategoryCriteria(in);
    }

    public CategoryCriteria[] newArray(int size) {
      return new CategoryCriteria[size];
    }
  };

  @Nullable
  public static Criteria fromStringExtra(String extra) {
    return extra.equals("null") ? new CategoryCriteria() : IdCriteria.fromStringExtra(extra, CategoryCriteria.class);
  }
}
