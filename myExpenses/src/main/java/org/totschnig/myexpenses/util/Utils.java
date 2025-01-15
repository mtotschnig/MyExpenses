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

package org.totschnig.myexpenses.util;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.provider.DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE;
import static org.totschnig.myexpenses.provider.filter.OperationKt.LIKE_ESCAPE_CHAR;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.IconMarginSpan;
import android.util.Xml;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.squareup.phrase.Phrase;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.db2.RepositoryPartyKt;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.GrisbiImportTask;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.ui.UiUtils;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Util class with helper methods
 *
 * @author Michael Totschnig
 */
public class Utils {

  public static final String PLACEHOLDER_APP_NAME = "app_name";

  private Utils() {
  }

  @Nullable
  public static String getCountryFromTelephonyManager(Context context) {
    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    if (telephonyManager != null) {
      try {
        String userCountry = telephonyManager.getNetworkCountryIso();
        if (TextUtils.isEmpty(userCountry)) {
          userCountry = telephonyManager.getSimCountryIso();
        }
        return userCountry;
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  public static List<Map<String, String>> getProjectDependencies(Context context) {
    List<Map<String, String>> result = new ArrayList<>();
    addProjectDependencies(result, context.getResources().getXml(R.xml.project_dependencies));
    addProjectDependencies(result, context.getResources().getXml(R.xml.additional_dependencies));
    return result;
  }

  private static void addProjectDependencies(List<Map<String, String>> result, XmlPullParser xpp) {
    int eventType;
    try {
      eventType = xpp.getEventType();
      Map<String, String> project = null;
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          if (xpp.getName().equals("project")) {
            project = new HashMap<>();
          } else if (project != null) {
            String key = xpp.getName();
            xpp.next();
            project.put(key, xpp.getText());
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (xpp.getName().equals("project")) {
            result.add(project);
          }
        }
        eventType = xpp.next();
      }
    } catch (Exception e) {
      CrashHandler.report(e);
    }
  }

  public static char getDefaultDecimalSeparator() {
    char sep = '.';
    NumberFormat nfDLocal = NumberFormat.getNumberInstance();
    if (nfDLocal instanceof DecimalFormat) {
      DecimalFormatSymbols symbols = ((DecimalFormat) nfDLocal)
          .getDecimalFormatSymbols();
      sep = symbols.getDecimalSeparator();
    }
    return sep;
  }

  /**
   * <a href="http://www.ibm.com/developerworks/java/library/j-numberformat/">
   * http://www.ibm.com/developerworks/java/library/j-numberformat/</a>
   *
   * @return the float retrieved from the string or null if parse did not
   * succeed
   */
  public static @Nullable BigDecimal validateNumber(DecimalFormat df, String strFloat) {
    ParsePosition pp;
    pp = new ParsePosition(0);
    pp.setIndex(0);
    df.setParseBigDecimal(true);
    BigDecimal n = (BigDecimal) df.parse(strFloat, pp);
    if (strFloat.length() != pp.getIndex() || n == null) {
      return null;
    } else {
      return n;
    }
  }

  /**
   * @return a DecimalFormat with the number of fraction digits appropriate for
   * currency, and with the given separator, but without the currency
   * symbol appropriate for CSV and QIF export
   */
  @NonNull
  public static DecimalFormat getDecimalFormat(CurrencyUnit currency, char separator) {
    DecimalFormat nf = new DecimalFormat();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(separator);
    nf.setDecimalFormatSymbols(symbols);
    int fractionDigits = currency.getFractionDigits();
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    nf.setGroupingUsed(false);
    return nf;
  }

  /**
   * utility method that calls formatter for date
   *
   * @param date unixEpoch
   * @return formatted string
   */
  public static String convDateTime(long date, DateFormat format) {
    return format.format(new Date(date * 1000L));
  }

  @NonNull
  public static Currency getSaveDefault() {
    try {
      return Currency.getInstance(Locale.getDefault());
    } catch (NullPointerException | IllegalArgumentException ex) {
      return Currency.getInstance(new Locale("en", "US"));
    }
  }

  @Nullable
  public static Currency getInstance(@Nullable String strCurrency) {
    if (strCurrency != null) {
      if (strCurrency.equals(AGGREGATE_HOME_CURRENCY_CODE)) {
        throw new IllegalArgumentException("AGGREGATE_HOME_CURRENCY_CODE needs to be resolved upfront");
      }
      try {
        return Currency.getInstance(strCurrency);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return null;
  }

  /**
   * @param context The application's environment.
   * @param intent  The Intent action to check for availability.
   * @return True if an Intent with the specified action can be sent and
   * responded to, false otherwise.
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
    return intent.resolveActivity(context.getPackageManager()) != null;
  }

  public static boolean isIntentReceiverAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent, 0);
    return !list.isEmpty();
  }

  public static boolean isComAndroidVendingInstalled(Context context) {
    PackageManager pm = context.getPackageManager();
    try {
      pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
    return true;
  }

  public static long getDaysSinceInstall(Context context) {
    return (System.currentTimeMillis() - getInstallTime(context)) / DAY_IN_MILLIS;
  }

  public static long getInstallTime(Context context) {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
          .firstInstallTime;
    } catch (NameNotFoundException e) {
      return 0;
    }
  }

  public static long getDaysSinceUpdate(Context context) {
    try {
      return (System.currentTimeMillis() -
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
              .lastUpdateTime) / DAY_IN_MILLIS;
    } catch (NameNotFoundException e) {
      return 0;
    }
  }

  /**
   * get a value from extras that could be null
   */
  public static long getFromExtra(@Nullable Bundle extras, String key, long defaultValue) {
    return (extras == null) ? defaultValue : extras.getLong(key, defaultValue);
  }

  @SuppressLint("DefaultLocale")
  public static String toLocalizedString(int i) {
    return String.format("%d", i);
  }

  public static String md5(String s) {
    try {
      // Create MD5 Hash
      MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
      digest.update(s.getBytes());
      byte[] messageDigest = digest.digest();

      // Create Hex String
      StringBuilder hexString = new StringBuilder();
      for (byte b : messageDigest) hexString.append(Integer.toHexString(0xFF & b));
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      CrashHandler.report(e);
    }
    return "";
  }

  public static DateFormat getFrameworkDateFormatSafe(Context context) {
    try {
      return android.text.format.DateFormat.getDateFormat(context);
    } catch (Exception e) {
      CrashHandler.report(e);
      //java.lang.SecurityException: Requires READ_PHONE_STATE observed on HUAWEI Y625-U13
      return java.text.DateFormat.getDateInstance(DateFormat.SHORT, localeFromContext(context));
    }
  }

  @SuppressLint("SimpleDateFormat")
  public static DateFormat getDateFormatSafe(Context context) {
    String custom = ((MyApplication) context.getApplicationContext()).getAppComponent().prefHandler().getString(PrefKey.CUSTOM_DATE_FORMAT,"");
    if (!"".equals(custom)) {
      try {
        return new SimpleDateFormat(custom, localeFromContext(context));
      } catch (Exception e) {
        CrashHandler.report(e);
      }
    }
    return getFrameworkDateFormatSafe(context);
  }

  public static DateFormat localizedYearLessDateFormat(Context context) {
    DateFormat dateFormat = getDateFormatSafe(context);
    if (dateFormat instanceof SimpleDateFormat) {
      final String contextPattern = ((SimpleDateFormat) dateFormat).toPattern();
      String yearLessPattern = contextPattern.replaceAll("\\W?[Yy]+\\W?", "");
      return new SimpleDateFormat(yearLessPattern, localeFromContext(context));
    } else {
      return dateFormat;
    }
  }

  public static Locale localeFromContext(Context context) {
    return context.getResources().getConfiguration().locale;
  }

  /**
   * @return Locale en-CA has "y-MM-dd" (2019-06-18) as short date format, which does not fit into
   * the space, we allocate for dates when transactions are not grouped, we replace the year part with "yy"
   */
  public static DateFormat ensureDateFormatWithShortYear(Context context) {
    DateFormat dateFormat = getDateFormatSafe(context);
    if (dateFormat instanceof SimpleDateFormat) {
      final String contextPattern = ((SimpleDateFormat) dateFormat).toPattern();
      String shortPattern = contextPattern.replaceAll("y+", "yy");
      return new SimpleDateFormat(shortPattern, localeFromContext(context));
    } else {
      return dateFormat;
    }
  }

  public static Result<Pair<CategoryTree, ArrayList<String>>> analyzeGrisbiFileWithSAX(InputStream is) {
    GrisbiHandler handler = new GrisbiHandler();
    try {
      Xml.parse(is, Xml.Encoding.UTF_8, handler);
    } catch (IOException e) {
      return Result.ofFailure(R.string.parse_error_other_exception, e.getMessage());
    } catch (GrisbiHandler.FileVersionNotSupportedException e) {
      return Result.ofFailure(R.string.parse_error_grisbi_version_not_supported, e.getMessage());
    } catch (SAXException e) {
      return Result.ofFailure(R.string.parse_error_parse_exception);
    }
    return handler.getResult();
  }

  public static int importParties(Repository repository, ArrayList<String> partiesList,
                                  GrisbiImportTask task) {
    int total = 0;
    for (int i = 0, partiesListSize = partiesList.size(); i < partiesListSize; i++) {
      String party = partiesList.get(i);
      if (RepositoryPartyKt.findParty(repository, party) == null) {
        RepositoryPartyKt.createParty(repository, party);
        total++;
      }
      if (task != null && i % 10 == 0) {
        task.publishProgress(i);
      }
    }
    return total;
  }


  public static CharSequence getTextWithAppName(Context ctx, int resId) {
    return Phrase.from(ctx, resId).put(PLACEHOLDER_APP_NAME, ctx.getString(R.string.app_name)).format();
  }

  public static CharSequence getTellAFriendMessage(Context ctx) {
    return Phrase.from(ctx, R.string.tell_a_friend_message)
        .put(PLACEHOLDER_APP_NAME, ctx.getString(R.string.app_name))
        .put("platform", DistributionHelper.getPlatform())
        .put("website", ctx.getString(R.string.website)).format();
  }

  /**
   * @return a representation of str converted to lower case, Unicode
   * normalization applied and markers removed this allows
   * case-insensitive comparison for non-ascii and non-latin strings
   */
  public static String normalize(String str) {
    //noinspection DefaultLocale
    str = str.toLowerCase();
    // Credits: http://stackoverflow.com/a/3322174/1199911
    return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{M}",
        "");
  }

  public static String escapeSqlLikeExpression(String str) {
    return str
        .replace(LIKE_ESCAPE_CHAR,
            LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
        .replace("%", LIKE_ESCAPE_CHAR + "%")
        .replace("_", LIKE_ESCAPE_CHAR + "_");
  }

  public static String printDebug(Object[] objects) {
    if (objects == null) {
      return "null";
    }
    StringBuilder result = new StringBuilder();
    for (Object object : objects) {
      if (!result.toString().equals(""))
        result.append(",");
      result.append(object == null ? "null" : object.toString());
    }
    return result.toString();
  }

  /**
   * filters out the '/' character and characters of type {@link Character#SURROGATE} or
   * {@link java.lang.Character#OTHER_SYMBOL}, meant primarily to skip emojis
   */
  public static String escapeForFileName(String in) {
    return in.replace("/", "").replaceAll("\\p{Cs}", "").replaceAll("\\p{So}", "");
  }

  public static int getFirstDayOfWeek(Locale locale) {
    return new GregorianCalendar(locale).getFirstDayOfWeek();
  }

  public static void configureGroupingMenu(SubMenu groupingMenu, Grouping currentGrouping) {
    (switch (currentGrouping) {
          case DAY -> groupingMenu.findItem(R.id.GROUPING_DAY_COMMAND);
          case WEEK -> groupingMenu.findItem(R.id.GROUPING_WEEK_COMMAND);
          case MONTH -> groupingMenu.findItem(R.id.GROUPING_MONTH_COMMAND);
          case YEAR -> groupingMenu.findItem(R.id.GROUPING_YEAR_COMMAND);
          default -> groupingMenu.findItem(R.id.GROUPING_NONE_COMMAND);
        }).setChecked(true);
  }

  @Nullable
  public static Grouping getGroupingFromMenuItemId(int id) {
    if (id == R.id.GROUPING_NONE_COMMAND) {
      return Grouping.NONE;
    } else if (id == R.id.GROUPING_DAY_COMMAND) {
      return Grouping.DAY;
    } else if (id == R.id.GROUPING_WEEK_COMMAND) {
      return Grouping.WEEK;
    } else if (id == R.id.GROUPING_MONTH_COMMAND) {
      return Grouping.MONTH;
    } else if (id == R.id.GROUPING_YEAR_COMMAND) {
      return Grouping.YEAR;
    }
    return null;
  }

  public static void requireLoader(LoaderManager manager, int loaderId, Bundle args,
                                   LoaderManager.LoaderCallbacks<Cursor> callback) {
    final Loader<Cursor> loader = manager.getLoader(loaderId);
    if (loader != null && !loader.isReset()) {
      manager.restartLoader(loaderId, args, callback);
    } else {
      manager.initLoader(loaderId, args, callback);
    }
  }

  public static CharSequence makeBulletList(Context ctx, List<CharSequence> lines, int icon) {
    Bitmap scaledBitmap = scaledBitmap(ctx, icon);
    SpannableStringBuilder sb = new SpannableStringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      sb.append(withIconMargin(scaledBitmap, lines.get(i), i < lines.size() - 1));
    }
    return sb;
  }

  private static CharSequence withIconMargin(Bitmap bitmap, CharSequence text, boolean withNewLine) {
    Spannable spannable = new SpannableString(withNewLine ? TextUtils.concat(text, "\n") : text);
    spannable.setSpan(new IconMarginSpan(bitmap, 25), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  private static Bitmap scaledBitmap(Context ctx, int icon) {
    InsetDrawable drawable = new InsetDrawable(
        UiUtils.getTintedDrawableForContext(ctx, icon), 0, 20, 0, 0);
    Bitmap bitmap = UiUtils.drawableToBitmap(drawable);
    return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.5),
        (int) (bitmap.getHeight() * 0.5), true);
  }

  public static String getSimpleClassNameFromComponentName(@NonNull ComponentName componentName) {
    String className = componentName.getShortClassName();
    return className.substring(className.lastIndexOf(".") + 1);
  }

  //http://stackoverflow.com/a/28565320/1199911
  public static Throwable getCause(Throwable e) {
    Throwable cause;
    Throwable result = e;

    while (null != (cause = result.getCause()) && (result != cause)) {
      result = cause;
    }
    return result;
  }

  public static boolean isKnownCurrency(String currencyCode) {
    try {
      CurrencyEnum.valueOf(currencyCode);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
