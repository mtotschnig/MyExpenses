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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class MethodCriteria extends IdCriteria {

  public MethodCriteria(long id, String label) {
    super(MyApplication.getInstance().getString(R.string.method),
        DatabaseConstants.KEY_METHODID, id, label);
  }

  public MethodCriteria(Parcel in) {
    super(in);
  }

  public static final Parcelable.Creator<MethodCriteria> CREATOR = new Parcelable.Creator<MethodCriteria>() {
    public MethodCriteria createFromParcel(Parcel in) {
        return new MethodCriteria(in);
    }

    public MethodCriteria[] newArray(int size) {
        return new MethodCriteria[size];
    }
  };
  public static MethodCriteria fromStringExtra(String extra) {
    int sepIndex = extra.indexOf(EXTRA_SEPARATOR);
    long id = Long.parseLong(extra.substring(0, sepIndex));
    String label = extra.substring(sepIndex+1);
    return new MethodCriteria(id,label);
  }
}
