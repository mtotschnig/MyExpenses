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
