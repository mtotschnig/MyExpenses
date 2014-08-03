/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package org.totschnig.myexpenses.provider.filter;

import java.util.Arrays;
import java.util.LinkedList;
import org.totschnig.myexpenses.util.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

public class WhereFilter {

  public static final String FILTER_EXTRA = "filter";
  public static final String ID_EXTRA = "id";
  public static final String SORT_ORDER_EXTRA = "sort_order";

  public static final String FILTER_TITLE_PREF = "filterTitle";
  public static final String FILTER_LENGTH_PREF = "filterLength";
  public static final String FILTER_CRITERIA_PREF = "filterCriteria";
  public static final String FILTER_SORT_ORDER_PREF = "filterSortOrder";
  public static final String LIKE_ESCAPE_CHAR = "\\";

  private SparseArray<Criteria> criterias= new SparseArray<Criteria>();
  private final LinkedList<String> sorts = new LinkedList<String>();

  public WhereFilter() {
  }

  public WhereFilter(SparseArray<Parcelable> sparseArray) {
    for (int i = 0; i < sparseArray.size(); i++) {
      put(sparseArray.keyAt(i),(Criteria) sparseArray.valueAt(i));
    }
  }

  public WhereFilter asc(String column) {
    sorts.add(column + " asc");
    return this;
  }

  public WhereFilter desc(String column) {
    sorts.add(column + " desc");
    return this;
  }

  public String getSelection() {
    StringBuilder sb = new StringBuilder();
    for(int i = 0, nsize = criterias.size(); i < nsize; i++) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(criterias.valueAt(i).getSelection());
    }
    return sb.toString().trim();
  }

  public String[] getSelectionArgs() {
    String[] args = new String[0];
    for(int i = 0, nsize = criterias.size(); i < nsize; i++) {
      args = Utils.joinArrays(args, criterias.valueAt(i).getSelectionArgs());
    }
    return args;
  }

  public Criteria get(int id) {
    return criterias.get(id);
  }

  public void put(int id, Criteria criteria) {
    criterias.put(id, criteria);
  }

  public void remove(int id) {
    criterias.remove(id);
  }

  public void clear() {
    criterias.clear();
    sorts.clear();
  }

  public static WhereFilter empty() {
    return new WhereFilter();
  }

  public String getSortOrder() {
    StringBuilder sb = new StringBuilder();
    for (String o : sorts) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(o);
    }
    return sb.toString();
  }

  public void resetSort() {
    sorts.clear();
  }

//  public void toSharedPreferences(SharedPreferences preferences) {
//    Editor e = preferences.edit();
//    int count = criterias.size();
//    e.putInt(FILTER_LENGTH_PREF, count);
//    for (int i = 0; i < count; i++) {
//      e.putString(FILTER_CRITERIA_PREF + i, criterias.get(i).toStringExtra());
//    }
//    e.putString(FILTER_SORT_ORDER_PREF, getSortOrder());
//    e.commit();
//  }

//  public static WhereFilter fromSharedPreferences(SharedPreferences preferences) {
//    String title = preferences.getString(FILTER_TITLE_PREF, "");
//    WhereFilter filter = new WhereFilter(title);
//    int count = preferences.getInt(FILTER_LENGTH_PREF, 0);
//    if (count > 0) {
//      for (int i = 0; i < count; i++) {
//        String criteria = preferences.getString(FILTER_CRITERIA_PREF + i, "");
//        if (criteria.length() > 0) {
//          filter.put(Criteria.fromStringExtra(criteria));
//        }
//      }
//    }
//    String sortOrder = preferences.getString(FILTER_SORT_ORDER_PREF, "");
//    String[] orders = sortOrder.split(",");
//    if (orders != null && orders.length > 0) {
//      filter.sorts.addAll(Arrays.asList(orders));
//    }
//    return filter;
//  }

  public boolean isEmpty() {
    return criterias.size()==0;
  }

  public static enum Operation {
    NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW(
        "BETWEEN ? AND ?"), ISNULL("is NULL"), LIKE("LIKE ? ESCAPE '" + LIKE_ESCAPE_CHAR + "'");

    public final String op;

    private Operation(String op) {
      this.op = op;
    }
  }

  public SparseArray<Criteria> getCriteria() {
    return criterias;
  }

}
