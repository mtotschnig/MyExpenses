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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;

public class PayeeCriteria extends IdCriteria {
  static final String COLUMN = KEY_PAYEEID;

  public PayeeCriteria() {
    super();
  }

  public PayeeCriteria(String label, long... ids) {
    super(label, ids);
  }

  @SuppressWarnings("unused")
  public PayeeCriteria(String label, String... ids) {
    super(label, ids);
  }

  @Override
  public int getID() {
    return R.id.FILTER_PAYEE_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  private PayeeCriteria(Parcel in) {
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

  public static Criteria fromStringExtra(String extra) {
    return extra.equals("null") ? new PayeeCriteria() : IdCriteria.fromStringExtra(extra, PayeeCriteria.class);
  }

  @Override
  protected boolean shouldApplyToParts() {
    return false;
  }
}
