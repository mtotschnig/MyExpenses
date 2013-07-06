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
*/

//based on http://stackoverflow.com/a/7343721/1199911
package org.totschnig.myexpenses.util;

import android.database.Cursor;
import android.database.CursorWrapper;

public class AllButOneCursorWrapper extends CursorWrapper {
  int hiddenPosition;
  int mPos = -1;

  public AllButOneCursorWrapper(Cursor cursor,int hiddenPosition) {
    super(cursor);
    this.hiddenPosition = hiddenPosition;
  }
  
  @Override
  public int getCount() {
    return super.getCount()-1;
  }

  @Override
  public boolean moveToPosition(int pos) {
    int realPos = pos < hiddenPosition ? pos : pos+1;
    boolean moved = super.moveToPosition(realPos);
    if (moved) mPos = pos;
    return moved;
  }

  @Override
  public final boolean move(int offset) {
    return moveToPosition(mPos + offset);
  }

  @Override
  public final boolean moveToFirst() {
    return moveToPosition(0);
  }

  @Override
  public final boolean moveToLast() {
    return moveToPosition(getCount() - 1);
  }

  @Override
  public final boolean moveToNext() {
    return moveToPosition(mPos + 1);
  }

  @Override
  public final boolean moveToPrevious() {
    return moveToPosition(mPos - 1);
  }

  @Override
  public final boolean isFirst() {
    return mPos == 0 && getCount() != 0;
  }

  @Override
  public final boolean isLast() {
    int cnt = getCount();
    return mPos == (cnt - 1) && cnt != 0;
  }

  @Override
  public final boolean isBeforeFirst() {
    if (getCount() == 0) {
        return true;
    }
    return mPos == -1;
  }

  @Override
  public final boolean isAfterLast() {
    if (getCount() == 0) {
        return true;
    }
    return mPos == getCount();
  }

  @Override
  public int getPosition() {
    return mPos;
  }
}
