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

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class WhereFilter {

  public static final String LIKE_ESCAPE_CHAR = "\\";

  @NonNull private ArrayList<Criteria> criterias = new ArrayList<>();

  public WhereFilter() {
  }

  public WhereFilter(@NonNull ArrayList<Criteria> criterias) {
    this.criterias = criterias;
  }

  public String getSelectionForParents(String tableName) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      Criteria c = criterias.get(i);
      if (c != null) {
        if (sb.length() > 0) {
          sb.append(" AND ");
        }
        sb.append(c.getSelectionForParents(tableName));
      }
    }
    return sb.toString().trim();
  }

  public String getSelectionForParts(String tableName) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      Criteria c = criterias.get(i);
      if (c != null) {
        if (sb.length() > 0) {
          sb.append(" AND ");
        }
        sb.append(c.getSelectionForParts(tableName));
      }
    }
    return sb.toString().trim();
  }

  public String[] getSelectionArgs(boolean queryParts) {
    String[] args = new String[0];
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      Criteria c = criterias.get(i);
      if (c != null) {
        String[] critArgs = c.getSelectionArgs();
        if (queryParts || c.shouldApplyToParts()) {
          critArgs = Utils.joinArrays(critArgs, critArgs);
        }
        //we need to double each criteria since it is applied to parents and parts
        args = Utils.joinArrays(args, critArgs);
      }
    }
    return args;
  }

  @Nullable
  public Criteria get(int id) {
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      Criteria c = criterias.get(i);
      if (c.getID() == id) {
        return c;
      }
    }
    return  null;
  }

  @Nullable
  public Criteria get(String column) {
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      Criteria c = criterias.get(i);
      if (c.getColumn().equals(column)) {
        return c;
      }
    }
    return  null;
  }

  public void put(Criteria criteria) {
    if (criteria != null) {
      int existing = indexOf(criteria.getID());
      if ( existing > -1) {
        criterias.set(existing, criteria);
      } else {
        criterias.add(criteria);
      }
    }
  }

  private int indexOf(int id) {
    for (int i = 0, nsize = criterias.size(); i < nsize; i++) {
      if (criterias.get(i).getID() == id)
        return i;
    }
    return -1;
  }

  public void remove(int id) {
    int existing = indexOf(id);
    if (existing > -1) {
      criterias.remove(existing);
    }
  }

  public void clear() {
    criterias.clear();
  }

  public static WhereFilter empty() {
    return new WhereFilter();
  }

  public boolean isEmpty() {
    return criterias.size() == 0;
  }

  public enum Operation {
    NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW(
        "BETWEEN ? AND ?"), ISNULL("is NULL"), LIKE("LIKE ? ESCAPE '" + LIKE_ESCAPE_CHAR + "'"),
    IN(null);

    private final String op;

    Operation(String op) {
      this.op = op;
    }

    @NonNull
    public String getOp(int length) {
      if (this == Operation.IN) {
        StringBuilder sb = new StringBuilder();
        sb.append("IN (");
        for (int i = 0; i < length; i++) {
          sb.append("?");
          if (i < length - 1) {
            sb.append(",");
          }
        }
        sb.append(")");
        return sb.toString();
      }
      return op;
    }
  }

  public ArrayList<Criteria> getCriteria() {
    return criterias;
  }

}
