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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import ru.orangesoftware.financisto.activity.DateFilterActivity;
//import ru.orangesoftware.financisto.blotter.BlotterFilter;
//import ru.orangesoftware.financisto.utils.Utils;
//import ru.orangesoftware.financisto.datetime.PeriodType;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

public class WhereFilter {

  public static final String TITLE_EXTRA = "title";
  public static final String FILTER_EXTRA = "filter";
  public static final String SORT_ORDER_EXTRA = "sort_order";

  public static final String FILTER_TITLE_PREF = "filterTitle";
  public static final String FILTER_LENGTH_PREF = "filterLength";
  public static final String FILTER_CRITERIA_PREF = "filterCriteria";
  public static final String FILTER_SORT_ORDER_PREF = "filterSortOrder";

  private final String title;
  private final HashMap<Integer,Criteria> criterias = new HashMap<Integer,Criteria>();
  private final LinkedList<String> sorts = new LinkedList<String>();

  public WhereFilter(String title) {
    this.title = title;
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
    for (Map.Entry<Integer, Criteria> entry : criterias.entrySet()) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(entry.getValue().getSelection());
    }
    return sb.toString().trim();
  }

  public String[] getSelectionArgs() {
    String[] args = new String[0];
    for (Map.Entry<Integer, Criteria> entry : criterias.entrySet()) {
      args = Utils.joinArrays(args, entry.getValue().getSelectionArgs());
    }
    return args;
  }

  public Criteria get(Integer id) {
    return criterias.get(id);
  }

  public void put(Integer id, Criteria criteria) {
    criterias.put(id, criteria);
  }

  public Criteria remove(Integer id) {
    return criterias.remove(id);
  }

  public void clear() {
    criterias.clear();
    sorts.clear();
  }

  public static WhereFilter empty() {
    return new WhereFilter("");
  }

  public void toBundle(Bundle bundle) {
    String[] extras = new String[criterias.size()];
    for (int i = 0; i < extras.length; i++) {
      extras[i] = criterias.get(i).toStringExtra();
    }
    bundle.putString(TITLE_EXTRA, title);
    bundle.putStringArray(FILTER_EXTRA, extras);
    bundle.putString(SORT_ORDER_EXTRA, getSortOrder());
  }

//  public static WhereFilter fromBundle(Bundle bundle) {
//    String title = bundle.getString(TITLE_EXTRA);
//    WhereFilter filter = new WhereFilter(title);
//    String[] a = bundle.getStringArray(FILTER_EXTRA);
//    if (a != null) {
//      for (String s : a) {
//        filter.put(Criteria.fromStringExtra(s));
//      }
//    }
//    String sortOrder = bundle.getString(SORT_ORDER_EXTRA);
//    if (sortOrder != null) {
//      String[] orders = sortOrder.split(",");
//      if (orders != null && orders.length > 0) {
//        filter.sorts.addAll(Arrays.asList(orders));
//      }
//    }
//    return filter;
//  }

  public void toIntent(Intent intent) {
    Bundle bundle = intent.getExtras();
    if (bundle == null)
      bundle = new Bundle();
    toBundle(bundle);
    intent.replaceExtras(bundle);
  }

//  public static WhereFilter fromIntent(Intent intent) {
//    Bundle bundle = intent.getExtras();
//    if (bundle == null)
//      bundle = new Bundle();
//    return fromBundle(bundle);
//  }

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

  public void toSharedPreferences(SharedPreferences preferences) {
    Editor e = preferences.edit();
    int count = criterias.size();
    e.putString(FILTER_TITLE_PREF, title);
    e.putInt(FILTER_LENGTH_PREF, count);
    for (int i = 0; i < count; i++) {
      e.putString(FILTER_CRITERIA_PREF + i, criterias.get(i).toStringExtra());
    }
    e.putString(FILTER_SORT_ORDER_PREF, getSortOrder());
    e.commit();
  }

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

  public String getTitle() {
    return title;
  }

  public boolean isEmpty() {
    return criterias.isEmpty();
  }

  public static enum Operation {
    NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW(
        "BETWEEN ? AND ?"), ISNULL("is NULL"), LIKE("LIKE ?");

    public final String op;

    private Operation(String op) {
      this.op = op;
    }
  }

}
