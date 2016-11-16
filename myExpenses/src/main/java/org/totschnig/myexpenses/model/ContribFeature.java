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

import android.content.Context;
import android.text.Html;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Utils;

public enum ContribFeature {
  ACCOUNTS_UNLIMITED(false),
  PLANS_UNLIMITED(false),
  SECURITY_QUESTION,
  SPLIT_TRANSACTION,
  DISTRIBUTION,
  TEMPLATE_WIDGET,
  ATTACH_PICTURE,
  AD_FREE(false),
  CSV_IMPORT(true, true),
  AUTO_BACKUP(true, true) {
    @Override
    public String buildUsagesString(Context ctx, int usagesLeft) {
      return usagesLeft > 0 ? ctx.getString(R.string.warning_auto_backup_limited_trial,usagesLeft) :
          ctx.getString(R.string.warning_auto_backup_limit_reached);
    }
  };

  ContribFeature() {
    this(true);
  }

  ContribFeature(boolean hasTrial) {
    this(hasTrial, false);
  }

  ContribFeature(boolean hasTrial, boolean isExtended) {
    this.hasTrial = hasTrial;
    this.isExtended = LicenceHandler.HAS_EXTENDED ? isExtended : false;
  }

  private boolean hasTrial;
  private boolean isExtended;
  /**
   * how many times contrib features can be used for free
   */
  public static final int USAGES_LIMIT = 5000;

  public String toString() {
    return name().toLowerCase(Locale.US);
  }

  public int getUsages() {
    return MyApplication.getInstance().getSettings()
        .getInt(getPrefKey(), 0);
  }

  public int recordUsage() {
    if (!hasAccess()) {
      int usages = getUsages() + 1;
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings()
              .edit().putInt(getPrefKey(), usages));
      return USAGES_LIMIT - usages;
    }
    return USAGES_LIMIT;
  }

  private String getPrefKey() {
    return "FEATURE_USAGES_" + name();
  }

  public int usagesLeft() {
    return hasTrial ? USAGES_LIMIT - getUsages() : 0;
  }

  public boolean hasAccess() {
    LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
    return true;
  }

  public String buildRequiresString(Context ctx) {
    return ctx.getString(R.string.contrib_key_requires, buildKeyFullName(ctx, isExtended));
  }

  public String buildFullInfoString(Context ctx,int usagesLeft) {
    return ctx.getString(
        isExtended ? R.string.dialog_contrib_extended_feature : R.string.dialog_contrib_premium_feature,
        "<i>" + ctx.getString(ctx.getResources().getIdentifier(
            "contrib_feature_" + toString() + "_label", "string", ctx.getPackageName())) + "</i>") + " " +
        buildUsagesString(ctx,usagesLeft);
  }

  public static String buildKeyFullName(Context ctx, boolean extended) {
    return Utils.concatResStrings(ctx, " ", R.string.app_name,
        extended ? R.string.extended_key : R.string.contrib_key);
  }

  public String buildUsagesString(Context ctx, int usagesLeft) {
    return usagesLeft > 0 ?
        ctx.getResources().getQuantityString(R.plurals.dialog_contrib_usage_count, usagesLeft, usagesLeft) :
        ctx.getString(R.string.dialog_contrib_no_usages_left);
  }

  public CharSequence buildRemoveLimitation(Context ctx,boolean asHTML) {
    String keyName = buildKeyFullName(ctx,isExtended);
    if (asHTML) {
      keyName = "<i>" +  keyName + "</i>";
    }
    String result = ctx.getString(R.string.dialog_contrib_reminder_remove_limitation, keyName);
    return asHTML ? Html.fromHtml(result) : result;
  }

  public boolean isExtended() {
    return isExtended;
  }

  public boolean hasTrial() {
    return hasTrial;
  }
}