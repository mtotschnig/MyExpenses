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
public class PayeeCriteria extends IdCriteria {

  public PayeeCriteria(long id, String label) {
    super(MyApplication.getInstance().getString(R.string.payer_or_payee),
        DatabaseConstants.KEY_PAYEEID, id, label);
  }

  public PayeeCriteria(Parcel in) {
    super(in);
  }

  public static final Parcelable.Creator<PayeeCriteria> CREATOR = new Parcelable.Creator<PayeeCriteria>() {
    public PayeeCriteria createFromParcel(Parcel in) {
        return new PayeeCriteria(in);
    }

    public PayeeCriteria[] newArray(int size) {
        return new PayeeCriteria[size];
    }
  };
  public static PayeeCriteria fromStringExtra(String extra) {
    int sepIndex = extra.indexOf(EXTRA_SEPARATOR);
    long id = Long.parseLong(extra.substring(0, sepIndex));
    String label = extra.substring(sepIndex+1);
    return new PayeeCriteria(id,label);
  }
}
