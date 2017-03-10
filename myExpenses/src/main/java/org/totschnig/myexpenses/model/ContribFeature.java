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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.Html;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.LicenceHandler;

import java.util.Date;
import java.util.Locale;

public enum ContribFeature {
  ACCOUNTS_UNLIMITED(false) {
    private int FREE_ACCOUNTS = 5;
    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_accounts, FREE_ACCOUNTS, currentLicence);
    }
  },
  PLANS_UNLIMITED(false){
    private int FREE_PLANS = 5;
    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_plans, FREE_PLANS, currentLicence);
    }
  },
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
    public String buildUsagesLefString(Context ctx) {
      int usagesLeft = usagesLeft();
      return usagesLeft > 0 ? ctx.getString(R.string.warning_auto_backup_limited_trial, usagesLeft) :
          ctx.getString(R.string.warning_auto_backup_limit_reached);
    }
  },
  SYNCHRONIZATION(true, true) {
    private String PREF_KEY = "FEATURE_SYNCHRONIZATION_FIRST_USAGE";
    private int TRIAL_DURATION_DAYS = 10;
    private long TRIAL_DURATION_MILLIS = (TRIAL_DURATION_DAYS * 24 * 60) * 60 * 1000;

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
      return getStartOfTrial(now) + TRIAL_DURATION_MILLIS;
    }

    @Override
    public String buildUsagesLefString(Context ctx) {
      long now = System.currentTimeMillis();
      long endOfTrial = getEndOfTrial(now);
      if (endOfTrial < now) {
        return ctx.getString(R.string.warning_synchronization_limit_reached);
      } else {
        return ctx.getString(R.string.warning_synchronization_limited_trial,
            android.text.format.DateFormat.getDateFormat(ctx).format(new Date(endOfTrial)));
      }
    }

    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_synchronization, TRIAL_DURATION_DAYS, currentLicence);
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
    this.isExtended = LicenceHandler.HAS_EXTENDED && isExtended;
  }

  private boolean hasTrial;
  private boolean isExtended;
  /**
   * how many times contrib features can be used for free
   */
  public static final int USAGES_LIMIT = 3;

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
    return ctx.getString(R.string.contrib_key_requires, buildKeyName(ctx, isExtended));
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

  public CharSequence buildFullInfoString(Context ctx) {
    return Html.fromHtml(ctx.getString(R.string.dialog_contrib_premium_feature,
        "<i>" + ctx.getString(getLabelResIdOrThrow(ctx)) + "</i>",
        buildKeyName(ctx, isExtended)) + " " +
        buildUsageLimitString(ctx));
  }

  public static String buildKeyName(Context ctx, boolean extended) {
    return ctx.getString(extended ? R.string.extended_key : R.string.contrib_key);
  }

  @SuppressLint("DefaultLocale")
  public CharSequence buildUsagesLefString(Context ctx) {
    int usagesLeft = usagesLeft();
    return ctx.getText(R.string.dialog_contrib_usage_count) + " : " +
        String.format("%d/%d", usagesLeft, USAGES_LIMIT);
  }

  public String buildUsageLimitString(Context context) {
    String currentLicence = getCurrentLicence(context);
    return context.getString(R.string.dialog_contrib_usage_limit, USAGES_LIMIT, currentLicence);
  }

  @NonNull
  protected String getCurrentLicence(Context context) {
    LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
    return licenceHandler.isExtendedEnabled() ?
        context.getString(R.string.extended_key) : (licenceHandler.isContribEnabled() ?
        context.getString(R.string.contrib_key) : context.getString(R.string.licence_status_free));
  }

  public CharSequence buildRemoveLimitation(Context ctx, boolean asHTML) {
    int resId = R.string.dialog_contrib_reminder_remove_limitation;
    return asHTML ? ctx.getText(resId) : ctx.getString(resId);
  }

  public boolean isExtended() {
    return isExtended;
  }

  public boolean hasTrial() {
    return hasTrial;
  }
}