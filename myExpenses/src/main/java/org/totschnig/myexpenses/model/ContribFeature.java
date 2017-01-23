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
import android.content.res.Resources;
import android.text.Html;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Utils;

import java.util.Date;
import java.util.Locale;

public enum ContribFeature {
  ACCOUNTS_UNLIMITED(false),
  PLANS_UNLIMITED(false),
  SECURITY_QUESTION,
  SPLIT_TRANSACTION,
  DISTRIBUTION,
  TEMPLATE_WIDGET,
  PRINT,
  ATTACH_PICTURE,
  AD_FREE(false),
  CSV_IMPORT(true, true),
  AUTO_BACKUP(true, true) {
    @Override
    public String buildUsagesString(Context ctx) {
      int usagesLeft = usagesLeft();
      return usagesLeft > 0 ? ctx.getString(R.string.warning_auto_backup_limited_trial, usagesLeft) :
          ctx.getString(R.string.warning_auto_backup_limit_reached);
    }
  },
  SYNCHRONIZATION(true, true) {
    private String PREF_KEY = "FEATURE_SYNCHRONIZATION_FIRST_USAGE";
    private long TRIAL_DURATION = (BuildConfig.DEBUG ? 10 : 10 * 24 * 60) * 60 * 1000;

    @Override
    public int recordUsage() {
      if (!hasAccess()) {
        long now = System.currentTimeMillis();
        if (getStartOfTrial(0L) == 0L) {
          MyApplication.getInstance().getSettings().edit().putLong(PREF_KEY, now).apply();
        }
        if (getEndOfTrial(now) < now) {
          return 0;
        }
      }
      return 1;
    }

    @Override
    public int usagesLeft() {
      long now = System.currentTimeMillis();
      return getEndOfTrial(now) < now ? 0 : 1;
    }

    private long getStartOfTrial(long defaultValue) {
      return MyApplication.getInstance().getSettings().getLong(PREF_KEY, defaultValue);
    }

    private long getEndOfTrial(long now) {
      return getStartOfTrial(now) + TRIAL_DURATION;
    }

    @Override
    public String buildUsagesString(Context ctx) {
      long now = System.currentTimeMillis();
      long endOfTrial = getEndOfTrial(now);
      if (endOfTrial < now) {
        return ctx.getString(R.string.warning_synchronization_limit_reached);
      } else {
        return ctx.getString(R.string.warning_synchronization_limited_trial,
            android.text.format.DateFormat.getDateFormat(ctx).format(new Date(endOfTrial)));
      }
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
  public static final int USAGES_LIMIT = 10;

  public String toString() {
    return name().toLowerCase(Locale.US);
  }

  public int getUsages() {
    return MyApplication.getInstance().getSettings()
        .getInt(getPrefKey(), 0);
  }

  /**
   * @return number of remaining usages (> 0, if usage still possible, <= 0 if not)
   */
  public int recordUsage() {
    if (!hasAccess()) {
      int usages = getUsages() + 1;
      MyApplication.getInstance().getSettings().edit()
          .putInt(getPrefKey(), usages)
          .apply();
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

  /**
   * @return if user has licence that includes feature
   */
  public boolean hasAccess() {
    if (BuildConfig.BUILD_TYPE.equals("beta")) {
      return true;
    }
    LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
    return isExtended ? licenceHandler.isExtendedEnabled() :
        licenceHandler.isContribEnabled();
  }

  /**
   * @return user either has access through licence or through trial
   */
  public boolean isAvailable() {
    return hasAccess() || usagesLeft() > 0;
  }

  public String buildRequiresString(Context ctx) {
    return ctx.getString(R.string.contrib_key_requires, buildKeyFullName(ctx, isExtended));
  }

  public int getLabelResIdOrThrow(Context ctx) {
    String name = "contrib_feature_" + toString() + "_label";
    int resId = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
    if (resId == 0) {
      throw new Resources.NotFoundException(name);
    }
    return resId;
  }

  public int getLimitReachedWarningResIdOrThrow(Context ctx) {
    String name = "warning_" + toString() + "_limit_reached";
    int resId = ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
    if (resId == 0) {
      throw new Resources.NotFoundException(name);
    }
    return resId;
  }

  public String buildFullInfoString(Context ctx) {
    return ctx.getString(
        isExtended ? R.string.dialog_contrib_extended_feature : R.string.dialog_contrib_premium_feature,
        "<i>" + ctx.getString(getLabelResIdOrThrow(ctx)) + "</i>") + " " +
        buildUsagesString(ctx);
  }

  public static String buildKeyFullName(Context ctx, boolean extended) {
    return Utils.concatResStrings(ctx, " ", R.string.app_name,
        extended ? R.string.extended_key : R.string.contrib_key);
  }

  public String buildUsagesString(Context ctx) {
    int usagesLeft = usagesLeft();
    return usagesLeft > 0 ?
        ctx.getResources().getQuantityString(R.plurals.dialog_contrib_usage_count, usagesLeft, usagesLeft) :
        ctx.getString(R.string.dialog_contrib_no_usages_left);
  }

  public CharSequence buildRemoveLimitation(Context ctx, boolean asHTML) {
    String keyName = buildKeyFullName(ctx, isExtended);
    if (asHTML) {
      keyName = "<i>" + keyName + "</i>";
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