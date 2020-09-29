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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.Arrays;

import androidx.annotation.Nullable;

public abstract class IdCriteria extends Criteria {

  private final String label;

  IdCriteria(String label, long... ids) {
    this(label, longArrayToStringArray(ids));
  }

  IdCriteria(String label, String... ids) {
    super(WhereFilter.Operation.IN, ids);
    this.label = label;
  }

  protected static String[] longArrayToStringArray(long[] in) {
    String[] out = new String[in.length];
    for (int i = 0; i < in.length; i++) {
      out[i] = String.valueOf(in[i]);
    }
    return out;
  }

  IdCriteria(Parcel in) {
    super(in);
    label = in.readString();
  }

  IdCriteria() {
    super(WhereFilter.Operation.ISNULL);
    label = null;
  }

  @Override
  public String prettyPrint(Context context) {
    return operation == WhereFilter.Operation.ISNULL ?
        String.format("%s: %s", columnName2Label(context), context.getString(R.string.unmapped)) : label;
  }

  private String columnName2Label(Context context) {
    switch (getColumn()) {
      case DatabaseConstants.KEY_CATID: return context.getString(R.string.category);
      case DatabaseConstants.KEY_PAYEEID: return context.getString(R.string.payer_or_payee);
      case DatabaseConstants.KEY_METHODID: return context.getString(R.string.method);
    }
    return getColumn();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(label);
  }

  @Override
  public String toStringExtra() {
    return operation == WhereFilter.Operation.ISNULL ? "null" :
        escapeSeparator(label) + EXTRA_SEPARATOR + TextUtils.join(EXTRA_SEPARATOR, values);
  }

  @Nullable
  public static <T extends IdCriteria> T fromStringExtra(String extra, Class<T> clazz) {
    String[] extraParts = extra.split(EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP);
    if (extraParts.length < 2) {
      CrashHandler.report(String.format("Unparsable string extra %s for %s", Arrays.toString(extraParts), clazz.getName()));
      return null;
    };
    String[] ids = Arrays.copyOfRange(extraParts, 1, extraParts.length);
    String label = unescapeSeparator(extraParts[0]);
    try {
      return clazz.getConstructor(String.class, String[].class).newInstance(label, ids);
    } catch (Exception e) {
      throw new RuntimeException("Unable to find constructor for class " + clazz.getName());
    }
  }

  public String getLabel() {
    return label;
  }
}
