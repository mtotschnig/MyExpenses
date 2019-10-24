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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;

public class TransferCriteria extends IdCriteria {
  static final String COLUMN = KEY_TRANSFER_ACCOUNT;


  public TransferCriteria(String label, long... ids) {
    super(label, ids);
  }

  @SuppressWarnings("unused")
  public TransferCriteria(String label, String... ids) {
    super(label, ids);
  }

  @Override
  public String getSelection() {
    String selection = operation.getOp(values.length);
    return KEY_TRANSFER_PEER + " IS NOT NULL AND (" + getColumn() + " " + selection + " OR " + KEY_ACCOUNTID + " " + selection + ")";
  }

  @Override
  public String[] getSelectionArgs() {
    return Utils.joinArrays(values,values);
  }

  @Override
  public int getID() {
    return R.id.FILTER_TRANSFER_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  public TransferCriteria(Parcel in) {
    super(in);
  }

  public static final Creator<TransferCriteria> CREATOR = new Creator<TransferCriteria>() {
    public TransferCriteria createFromParcel(Parcel in) {
        return new TransferCriteria(in);
    }

    public TransferCriteria[] newArray(int size) {
        return new TransferCriteria[size];
    }
  };
  public static TransferCriteria fromStringExtra(String extra) {
    return IdCriteria.fromStringExtra(extra,TransferCriteria.class);
  }
}
