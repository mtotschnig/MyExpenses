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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.CrStatus;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;

public class CrStatusCriteria extends Criteria {
  static final String COLUMN = KEY_CR_STATUS;

  public CrStatusCriteria(String... values) {
    super(WhereFilter.Operation.IN, values);
  }

  @Override
  public int getID() {
    return R.id.FILTER_STATUS_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  public CrStatusCriteria(Parcel in) {
    super(in);
  }

  @Override
  public String prettyPrint(Context context) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      sb.append(context.getString(CrStatus.valueOf(values[i]).toStringRes()));
      if (i < values.length - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }

  public static final Parcelable.Creator<CrStatusCriteria> CREATOR = new Parcelable.Creator<CrStatusCriteria>() {
    public CrStatusCriteria createFromParcel(Parcel in) {
      return new CrStatusCriteria(in);
    }

    public CrStatusCriteria[] newArray(int size) {
      return new CrStatusCriteria[size];
    }
  };

  @Override
  public String toStringExtra() {
    return TextUtils.join(EXTRA_SEPARATOR, values);
  }

  public static CrStatusCriteria fromStringExtra(String extra) {
    return new CrStatusCriteria(extra.split(EXTRA_SEPARATOR));
  }

  @Override
  protected boolean shouldApplyToParts() {
    return false;
  }
}
