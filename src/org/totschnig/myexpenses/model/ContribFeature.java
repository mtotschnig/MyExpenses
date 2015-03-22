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

package org.totschnig.myexpenses.model;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

public enum ContribFeature  {
  ACCOUNTS_UNLIMITED(false),
  PLANS_UNLIMITED(false),
  SECURITY_QUESTION,
  SPLIT_TRANSACTION,
  DISTRIBUTION,
  TEMPLATE_WIDGET,
  PRINT;

  private ContribFeature() {
    this(true);
  }
  private ContribFeature(boolean hasTrial) {
    this.hasTrial = hasTrial;
  }
  public boolean hasTrial;
  /**
   * how many times contrib features can be used for free
   */
  public static int USAGES_LIMIT = 5;
  public String toString() {
    return name().toLowerCase(Locale.US);
  }
  public int getUsages() {
    return MyApplication.getInstance().getSettings()
      .getInt(getPrefKey(), 0);
  }
  public int recordUsage() {
    if (!MyApplication.getInstance().isContribEnabled()) {
      int usages = getUsages()+1;
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings()
            .edit().putInt(getPrefKey(), usages));
      return USAGES_LIMIT - usages;
    }
    return USAGES_LIMIT;
  }
  private String getPrefKey() {
    return "FEATURE_USAGES_"+name();
  }
  public int usagesLeft() {
    return hasTrial ? USAGES_LIMIT - getUsages() : 0;
  }
}