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

import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.os.Parcel;

public abstract class TextCriteria extends Criteria {
  private String searchString;
  public TextCriteria(String title, String columnName, String searchString) {
    super(columnName, WhereFilter.Operation.LIKE,
        "%" + Utils.esacapeSqlLikeExpression(searchString) + "%");
    this.searchString = searchString;
    this.title = title;
  }
  public TextCriteria(Parcel in) {
   super(in);
   searchString = in.readString();
  }
  @Override
  public String prettyPrint(Context context) {
    return prettyPrintInternal(searchString);
  }
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(searchString);
  }
  
  @Override
  public String toStringExtra() {
    return searchString;
  };
}
