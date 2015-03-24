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

/**
 * Created by IntelliJ IDEA. User: denis.solonenko Date: 12/17/12 9:06 PM
 */
public class IdCriteria extends Criteria {

  protected final String label;

  public IdCriteria(String title, String column, long id, String label) {
    super(column, WhereFilter.Operation.EQ, String
        .valueOf(id));
    this.label = label;
    this.title = title;
  }

  public IdCriteria(Parcel in) {
    super(in);
    label = in.readString();
  }

  @Override
  public String prettyPrint() {
    return prettyPrintInternal(label);
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(label);
  }
  
  @Override
  public String toStringExtra() {
    return getStringValue() + EXTRA_SEPARATOR + label;
  };
}
