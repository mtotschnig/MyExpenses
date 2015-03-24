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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.Parcel;
import android.os.Parcelable;

public class CrStatusCriteria extends Criteria {
  private int searchIndex;
  public CrStatusCriteria(int searchIndex) {
    super(DatabaseConstants.KEY_CR_STATUS, WhereFilter.Operation.EQ,
        CrStatus.values()[searchIndex].name());
    this.searchIndex = searchIndex;
    this.title = MyApplication.getInstance().getString(R.string.status);
  }
  public CrStatusCriteria(Parcel in) {
    super(in);
  }
  @Override
  public String prettyPrint() {
    return prettyPrintInternal(CrStatus.values()[searchIndex].toString());
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
    return String.valueOf(searchIndex);
  }
  public static CrStatusCriteria fromStringExtra(String filter) {
    return new CrStatusCriteria(Integer.parseInt(filter));
  };
}
