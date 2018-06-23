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

import android.content.Context;
import android.os.Parcel;
import android.text.TextUtils;

import java.util.Arrays;

public abstract class IdCriteria extends Criteria {

  protected final String label;

  public IdCriteria(String title, String column, String label, long... ids) {
    this(title, column, label, longArrayToStringArray(ids));
  }

  public IdCriteria(String title, String column, String label, String... ids) {
    super(column, WhereFilter.Operation.IN, ids);
    this.label = label;
    this.title = title;
  }

  private static String[] longArrayToStringArray(long[] in) {
    String[] out = new String[in.length];
    for (int i = 0; i < in.length; i++) {
      out[i] = String.valueOf(in[i]);
    }
    return out;
  }

  public IdCriteria(Parcel in) {
    super(in);
    label = in.readString();
  }

  @Override
  public String prettyPrint(Context context) {
    return label;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(label);
  }

  @Override
  public String toStringExtra() {
    return escapeSeparator(label) + EXTRA_SEPARATOR + TextUtils.join(EXTRA_SEPARATOR, values);
  }

  public static <T extends IdCriteria> T fromStringExtra(String extra, Class<T> clazz) {
    String[] extraParts = extra.split(EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP);
    String ids[] = Arrays.asList(extraParts).subList(1, extraParts.length).toArray(new String[extraParts.length - 1]);
    String label = unescapeSeparator(extraParts[0]);
    try {
      return clazz.getConstructor(String.class, String[].class).newInstance(label, ids);
    } catch (Exception e) {
      throw new RuntimeException("Unable to find constructor for class " + clazz.getName());
    }
  }
}
