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

import java.util.LinkedList;
import org.totschnig.myexpenses.util.Utils;

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
