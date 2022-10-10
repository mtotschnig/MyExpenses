package org.totschnig.myexpenses.util.cursor;

import android.database.Cursor;
import android.database.CursorWrapper;

public abstract class CursorWrapperHelper extends CursorWrapper {
  protected int mPos = -1;

  public CursorWrapperHelper(Cursor cursor) {
    super(cursor);
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
  @Override
  public boolean moveToPosition(int pos) {
    // Make sure position isn't past the end of the cursor
    final int count = getCount();
    if (pos >= count) {
      mPos = count;
      return false;
    }
    // Make sure position isn't before the beginning of the cursor
    if (pos < 0) {
      mPos = -1;
      return false;
    }

    boolean moved = super.moveToPosition(getMappedPosition(pos));
    if (moved)
      mPos = pos;
    return moved;
  }

  protected abstract int getMappedPosition(int pos);
}
