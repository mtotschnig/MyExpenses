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
import org.totschnig.myexpenses.provider.DatabaseConstants;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class SingleCategoryCriteria extends IdCriteria {

  public SingleCategoryCriteria(long categoryId, String label) {
    super(MyApplication.getInstance().getString(R.string.category),
        KEY_CATID,
        categoryId,
        label);
  }

  public SingleCategoryCriteria(Parcel in) {
    super(in);
  }

  @Override
  public String getSelection() {
    return KEY_CATID +" IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
        + TABLE_CATEGORIES + " WHERE " + KEY_PARENTID + " = ? OR "
        + KEY_ROWID + " = ?)";
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
  
  public static SingleCategoryCriteria fromStringExtra(String extra) {
    int sepIndex = extra.indexOf(EXTRA_SEPARATOR);
    long id = Long.parseLong(extra.substring(0, sepIndex));
    String label = extra.substring(sepIndex+1);
    return new SingleCategoryCriteria(id, label);
  }
}
