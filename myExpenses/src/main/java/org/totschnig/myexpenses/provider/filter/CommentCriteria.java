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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;

public class CommentCriteria extends TextCriteria {
  static final String COLUMN = KEY_COMMENT;

  public CommentCriteria(String searchString) {
    super(searchString);
  }

  @Override
  public int getID() {
    return R.id.FILTER_COMMENT_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  private CommentCriteria(Parcel in) {
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
