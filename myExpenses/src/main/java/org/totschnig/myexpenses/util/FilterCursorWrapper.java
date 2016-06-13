package org.totschnig.myexpenses.util;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.CursorWrapper;

public class FilterCursorWrapper extends CursorWrapperHelper {
  private ArrayList<Integer> filterMap;

  public void setFilterMap(ArrayList<Integer> list) {
    this.filterMap = list;
  }

  public FilterCursorWrapper(Cursor cursor) {
    super(cursor);
  }

  @Override
  public int getCount() {
    return filterMap.size();
  }

  protected int getMappedPosition(int pos) {
    return filterMap.get(pos);
  }

}