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

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.HtmlCompat;

import static org.totschnig.myexpenses.util.licence.LicenceStatus.CONTRIB;
import static org.totschnig.myexpenses.util.licence.LicenceStatus.EXTENDED;
import static org.totschnig.myexpenses.util.licence.LicenceStatus.PROFESSIONAL;

//TODO separate enum definition from handler
public enum ContribFeature {
  ACCOUNTS_UNLIMITED(TrialMode.NONE) {
    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_accounts, FREE_ACCOUNTS, currentLicence);
    }
  },
  PLANS_UNLIMITED(TrialMode.NONE) {
    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_plans, FREE_PLANS, currentLicence);
    }
  },
  SPLIT_TRANSACTION,
  DISTRIBUTION,
  PRINT,
  ATTACH_PICTURE,
  AD_FREE(TrialMode.NONE),
  CSV_IMPORT(TrialMode.NUMBER_OF_TIMES, EXTENDED),
  AUTO_BACKUP(TrialMode.NUMBER_OF_TIMES, EXTENDED) {
    @Override
    public String buildUsagesLefString(Context ctx, PrefHandler prefHandler) {
      int usagesLeft = usagesLeft(prefHandler);
      return usagesLeft > 0 ? ctx.getString(R.string.warning_auto_backup_limited_trial, usagesLeft) :
          getLimitReachedWarning(ctx);
    }
  },
  SYNCHRONIZATION(TrialMode.DURATION, EXTENDED),
  SPLIT_TEMPLATE(TrialMode.NONE, PROFESSIONAL) {
    @Override
    public String buildUsageLimitString(Context context) {
      String currentLicence = getCurrentLicence(context);
      return context.getString(R.string.dialog_contrib_usage_limit_split_templates, currentLicence);
    }
  },
  PRO_SUPPORT(TrialMode.NONE, PROFESSIONAL),
  ROADMAP_VOTING(TrialMode.NONE, PROFESSIONAL),
  HISTORY(TrialMode.NUMBER_OF_TIMES, PROFESSIONAL),
  BUDGET(TrialMode.DURATION, PROFESSIONAL),
  OCR(TrialMode.DURATION, PROFESSIONAL),
  WEB_UI(TrialMode.DURATION, PROFESSIONAL);

  private enum TrialMode {NONE, NUMBER_OF_TIMES, DURATION}

  ContribFeature() {
    this(TrialMode.NUMBER_OF_TIMES);
  }

  ContribFeature(TrialMode trialMode) {
    this(trialMode, CONTRIB);
  }

  ContribFeature(TrialMode trialMode, LicenceStatus licenceStatus) {
    this.trialMode = trialMode;
    this.licenceStatus = licenceStatus;
  }


  public static final int FREE_PLANS = 3;
  public static final int FREE_ACCOUNTS = 5;
  public static final int FREE_SPLIT_TEMPLATES = 1;
  private final int TRIAL_DURATION_DAYS = 30;
  private final long TRIAL_DURATION_MILLIS = (TRIAL_DURATION_DAYS * 24 * 60) * 60 * 1000L;

  private final TrialMode trialMode;
  private final LicenceStatus licenceStatus;
  /**
   * how many times contrib features can be used for free
   */
  public static final int USAGES_LIMIT = BuildConfig.DEBUG ? Integer.MAX_VALUE : 10;

  @NonNull
  public String toString() {
    return name().toLowerCase(Locale.US);
  }

  private int getUsages(PrefHandler prefHandler) {
    return prefHandler.getInt(getPrefKey(), 0);
  }

  /**
   * @return number of remaining usages (> 0, if usage still possible, <= 0 if not)
   */
  public int recordUsage(PrefHandler prefHandler, LicenceHandler licenceHandler) {
    if (!licenceHandler.hasAccessTo(this)) {
      if (trialMode == TrialMode.NUMBER_OF_TIMES) {
        int usages = getUsages(prefHandler) + 1;
        prefHandler.putInt(getPrefKey(), usages);
        return USAGES_LIMIT - usages;
      } else if (trialMode == TrialMode.DURATION) {
        long now = System.currentTimeMillis();
        if (getStartOfTrial(0L, prefHandler) == 0L) {
          prefHandler.putLong(getPrefKey(), now);
        }
        if (getEndOfTrial(now, prefHandler) < now) {
          return 0;
        }
      }
    }
    return USAGES_LIMIT;
  }


  private long getStartOfTrial(long defaultValue, PrefHandler prefHandler) {
    return prefHandler.getLong(getPrefKey(), defaultValue);
  }

  private long getEndOfTrial(long defaultValue, PrefHandler prefHandler) {
    return getStartOfTrial(defaultValue, prefHandler) + TRIAL_DURATION_MILLIS;
  }

  private String getPrefKey() {
    final String format = trialMode == TrialMode.DURATION ? "FEATURE_%s_FIRST_USAGE" : "FEATURE_USAGES_%s";
    return String.format(Locale.ROOT, format, name());
  }

  public int usagesLeft(PrefHandler prefHandler) {
    switch (trialMode) {
      case NUMBER_OF_TIMES:
        return USAGES_LIMIT - getUsages(prefHandler);
      case DURATION:
        long now = System.currentTimeMillis();
        return getEndOfTrial(now, prefHandler) < now ? 0 : 1;
      default:
        return 0;
    }
  }

  public String buildRequiresString(Context ctx) {
    return ctx.getString(R.string.contrib_key_requires, ctx.getString(getLicenceStatus().getResId()));
  }

  public int getLabelResIdOrThrow(Context ctx) {
    int resId = getLabelResId(ctx);
    if (resId == 0) {
      throw new IllegalStateException("Label not defined for ContribFeature " + this);
    }
    return resId;
  }

  @VisibleForTesting
  public int getLabelResId(Context ctx) {
    String name = "contrib_feature_" + toString() + "_label";
    return ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
  }

  public String getLimitReachedWarning(Context ctx) {
    return ctx.getString(R.string.warning_trial_limit_reached, ctx.getString(getLabelResIdOrThrow(ctx)));
  }

  public CharSequence buildFullInfoString(Context ctx) {
    return HtmlCompat.fromHtml(ctx.getString(R.string.dialog_contrib_premium_feature,
        "<i>" + ctx.getString(getLabelResIdOrThrow(ctx)) + "</i>",
        ctx.getString(getLicenceStatus().getResId())) + " " +
        buildUsageLimitString(ctx), HtmlCompat.FROM_HTML_MODE_LEGACY);
  }

  @SuppressLint("DefaultLocale")
  public CharSequence buildUsagesLefString(Context ctx, PrefHandler prefHandler) {
    if (trialMode == TrialMode.NUMBER_OF_TIMES) {
      int usagesLeft = usagesLeft(prefHandler);
      return ctx.getText(R.string.dialog_contrib_usage_count) + " : " +
          String.format("%d/%d", usagesLeft, USAGES_LIMIT);
    } else if (trialMode == TrialMode.DURATION) {
      long now = System.currentTimeMillis();
      long endOfTrial = getEndOfTrial(now, prefHandler);
      if (endOfTrial < now) {
        return getLimitReachedWarning(ctx);
      } else {
        return ctx.getString(R.string.warning_limited_trial, ctx.getString(getLabelResIdOrThrow(ctx)),
            Utils.getDateFormatSafe(ctx).format(new Date(endOfTrial)));
      }
    }
    throw new IllegalStateException();
  }

  public String buildUsageLimitString(Context context) {
    String currentLicence = getCurrentLicence(context);
    switch (trialMode) {
      case NUMBER_OF_TIMES:
        return context.getString(R.string.dialog_contrib_usage_limit, USAGES_LIMIT, currentLicence);
      case DURATION:
        return context.getString(R.string.dialog_contrib_usage_limit_synchronization, TRIAL_DURATION_DAYS, currentLicence);
      default:
        return "";
    }
  }

  @NonNull
  protected String getCurrentLicence(Context context) {
    LicenceStatus licenceStatus = MyApplication.getInstance().getLicenceHandler().getLicenceStatus();
    return context.getString(licenceStatus == null ? R.string.licence_status_free : licenceStatus.getResId());
  }

  public CharSequence buildRemoveLimitation(Context ctx, boolean asHTML) {
    int resId = R.string.dialog_contrib_reminder_remove_limitation;
    return asHTML ? Utils.getTextWithAppName(ctx, resId) :
        ctx.getString(resId).replace(String.format("{%s}", Utils.PLACEHOLDER_APP_NAME), ctx.getString(R.string.app_name));
  }

  public boolean isExtended() {
    return getLicenceStatus() == EXTENDED;
  }

  public boolean isProfessional() {
    return getLicenceStatus() == PROFESSIONAL;
  }

  public boolean hasTrial() {
    return trialMode != TrialMode.NONE;
  }

  public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }

}