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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;

public class CommentCriteria extends TextCriteria {
  public CommentCriteria(String searchString) {
    super(MyApplication.getInstance().getString(R.string.comment), DatabaseConstants.KEY_COMMENT, searchString);
  }

  public CommentCriteria(Parcel in) {
    super(in);
  }

  public static final Parcelable.Creator<CommentCriteria> CREATOR = new Parcelable.Creator<CommentCriteria>() {
    public CommentCriteria createFromParcel(Parcel in) {
      return new CommentCriteria(in);
    }

    public CommentCriteria[] newArray(int size) {
      return new CommentCriteria[size];
    }
  };

  public static CommentCriteria fromStringExtra(String extra) {
    return new CommentCriteria(extra);
  }
}
