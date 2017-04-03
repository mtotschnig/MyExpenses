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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.IconMarginSpan;
import android.util.SparseIntArray;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.task.GrisbiImportTask;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
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
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;

/**
 * Util class with helper methods
 *
 * @author Michael Totschnig
 */
public class Utils {

  public static Currency getLocalCurrency() {
    Currency result = null;
    TelephonyManager telephonyManager = (TelephonyManager) MyApplication.getInstance()
        .getSystemService(Context.TELEPHONY_SERVICE);
    if (telephonyManager != null) {
      try {
        String userCountry = telephonyManager.getNetworkCountryIso();
        if (TextUtils.isEmpty(userCountry)) {
          userCountry = telephonyManager.getSimCountryIso();
        }
        if (!TextUtils.isEmpty(userCountry)) {
          result = getSaveInstance(Currency.getInstance(new Locale("", userCountry)));
        }
      } catch (Exception e) {
        //fall back to currency from locale
      }
    }
    if (result == null) {
      try {
        //makeSure we know about the currency
        result = getSaveInstance(Currency.getInstance(Locale.getDefault()));
      } catch (IllegalArgumentException e) {
        result = Currency.getInstance("EUR");
      }
    }
    return result;
  }

  public enum Feature {
    ;

    public boolean isEnabled() {
      return true;
    }
  }

  /*
  from https://www.google.com/design/spec/style/color.html#color-color-palette
  maps the 500 color to the 700 color
   */
  static final SparseIntArray colorPrimaryDarkMap = new SparseIntArray() {
    {
      append(0xffF44336, 0xffD32F2F);
      append(0xffE91E63, 0xffC2185B);
      append(0xff9C27B0, 0xff7B1FA2);
      append(0xff673AB7, 0xff512DA8);
      append(0xff3F51B5, 0xff303F9F);
      append(0xff2196F3, 0xff1976D2);
      append(0xff03A9F4, 0xff0288D1);
      append(0xff00BCD4, 0xff0097A7);
      append(0xff009688, 0xff00796B);
      append(0xff4CAF50, 0xff388E3C);
      append(0xff8BC34A, 0xff689F38);
      append(0xffCDDC39, 0xffAFB42B);
      append(0xffFFEB3B, 0xffFBC02D);
      append(0xffFFC107, 0xffFFA000);
      append(0xffFF9800, 0xffF57C00);
      append(0xffFF5722, 0xffE64A19);
      append(0xff795548, 0xff5D4037);
      append(0xff9E9E9E, 0xff616161);
      append(0xff607D8B, 0xff455A64);
      append(0xff757575, 0xff424242); //aggregate theme light 600 800
      append(0xffBDBDBD, 0xff757575); //aggregate theme dark  400 600
    }
  };

  private Utils() {
  }

  public static boolean hasApiLevel(int checkVersion) {
    return Build.VERSION.SDK_INT >= checkVersion;
  }

  private static NumberFormat numberFormat;

  private static void initNumberFormat() {
    String prefFormat = PrefKey.CUSTOM_DECIMAL_FORMAT.getString("");
    if (!prefFormat.equals("")) {
      DecimalFormat nf = new DecimalFormat();
      try {
        nf.applyLocalizedPattern(prefFormat);
        numberFormat = nf;
      } catch (IllegalArgumentException e) {
        //fallback to default currency instance
        numberFormat = NumberFormat.getCurrencyInstance();
      }
    } else {
      numberFormat = NumberFormat.getCurrencyInstance();
    }
  }

  private static NumberFormat getNumberFormat() {
    if (numberFormat == null) {
      initNumberFormat();
    }
    return numberFormat;
  }

  public static void setNumberFormat(NumberFormat in) {
    numberFormat = in;
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

  public static String defaultOrderBy(String textColumn, PrefKey prefKey) {
    String currentSortOrder = prefKey.getString("USAGES");
    String sortOrder = textColumn + " COLLATE LOCALIZED";
    switch (currentSortOrder) {
      case ProtectedFragmentActivity.SORT_ORDER_USAGES:
        sortOrder = KEY_USAGES + " DESC, " + sortOrder;
        break;
      case ProtectedFragmentActivity.SORT_ORDER_LAST_USED:
        sortOrder = KEY_LAST_USED + " DESC, " + sortOrder;
        break;
      case ProtectedFragmentActivity.SORT_ORDER_CUSTOM:
        sortOrder = KEY_SORT_KEY + " ASC, " + sortOrder;
        break;
      case ProtectedFragmentActivity.SORT_ORDER_AMOUNT:
        sortOrder = "abs(" + KEY_AMOUNT + ") DESC, " + sortOrder;
        break;
      case ProtectedFragmentActivity.SORT_ORDER_NEXT_INSTANCE:
        sortOrder = null; //handled by PlanInfoCursorWrapper
        //default is textColumn
    }
    return sortOrder;
  }

  /**
   * <a href="http://www.ibm.com/developerworks/java/library/j-numberformat/">
   * http://www.ibm.com/developerworks/java/library/j-numberformat/</a>
   *
   * @param strFloat parsed as float with the number format defined in the locale
   * @return the float retrieved from the string or null if parse did not
   * succeed
   */
  public static BigDecimal validateNumber(DecimalFormat df, String strFloat) {
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
   * formats an amount with a currency
   *
   * @param money
   * @return formated string
   */
  public static String formatCurrency(Money money) {
    BigDecimal amount = money.getAmountMajor();
    Currency currency = money.getCurrency();
    return formatCurrency(amount, currency);
  }

  public static String formatCurrency(BigDecimal amount, Currency currency) {
    NumberFormat nf = getNumberFormat();
    int fractionDigits = Money.getFractionDigits(currency);
    nf.setCurrency(currency);
    if (fractionDigits <= 3) {
      nf.setMinimumFractionDigits(fractionDigits);
      nf.setMaximumFractionDigits(fractionDigits);
    } else {
      nf.setMaximumFractionDigits(fractionDigits);
    }
    return nf.format(amount);
  }

  public static Date dateFromSQL(String dateString) {
    try {
      return TransactionDatabase.dateFormat.parse(dateString);
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * @param currency
   * @param separator
   * @return a Decimalformat with the number of fraction digits appropriate for
   * currency, and with the given separator, but without the currency
   * symbol appropriate for CSV and QIF export
   */
  public static DecimalFormat getDecimalFormat(Currency currency, char separator) {
    DecimalFormat nf = new DecimalFormat();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(separator);
    nf.setDecimalFormatSymbols(symbols);
    int fractionDigits = currency.getDefaultFractionDigits();
    if (fractionDigits != -1) {
      nf.setMinimumFractionDigits(fractionDigits);
      nf.setMaximumFractionDigits(fractionDigits);
    } else {
      nf.setMaximumFractionDigits(Money.DEFAULTFRACTIONDIGITS);
    }
    nf.setGroupingUsed(false);
    return nf;
  }

  /**
   * utility method that calls formatters for date
   *
   * @param text
   * @return formated string
   */
  public static String convDate(String text, DateFormat format) {
    Date date = dateFromSQL(text);
    if (date == null)
      return text;
    else
      return format.format(date);
  }

  /**
   * utility method that calls formatters for date
   *
   * @param text unixEpochAsString
   * @return formated string
   */
  public static String convDateTime(String text, DateFormat format) {
    if (text == null) {
      return "???";
    }
    Date date;
    try {
      date = new Date(Long.valueOf(text) * 1000L);
    } catch (NumberFormatException e) {
      // legacy, the migration from date string to unix timestamp
      // might have gone wrong for some users
      try {
        date = TransactionDatabase.dateTimeFormat.parse(text);
      } catch (ParseException e1) {
        date = new Date();
      }
    }
    return format.format(date);
  }

  /**
   * utility method that calls formatters for amount this method is called from
   * adapters that give us the amount as String
   *
   * @param text     amount as String
   * @param currency
   * @return formated string
   */
  public static String convAmount(String text, Currency currency) {
    Long amount;
    try {
      amount = Long.valueOf(text);
    } catch (NumberFormatException e) {
      amount = 0L;
    }
    return convAmount(amount, currency);
  }

  public static Currency getSaveInstance(String strCurrency) {
    Currency c;
    try {
      c = Currency.getInstance(strCurrency);
    } catch (IllegalArgumentException e) {
      Timber.e("%s is not defined in ISO 4217", strCurrency);
      c = Currency.getInstance(Locale.getDefault());
    }
    return getSaveInstance(c);
  }

  public static Currency getSaveInstance(Currency currency) {
    try {
      CurrencyEnum.valueOf(currency.getCurrencyCode());
      return currency;
    } catch (IllegalArgumentException e) {
      return Currency.getInstance("EUR");
    }
  }

  /**
   * utility method that calls formatters for amount this method can be called
   * directly with Long values retrieved from db
   *
   * @param amount
   * @param currency
   * @return formated string
   */
  public static String convAmount(Long amount, Currency currency) {
    return formatCurrency(new Money(currency, amount));
  }

  public static void setBackgroundFilter(View v, int c) {
    v.getBackground().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
  }

  /**
   * Indicates whether the specified action can be used as an intent. This
   * method queries the package manager for installed packages that can respond
   * to an intent with the specified action. If no suitable package is found,
   * this method returns false.
   * <p>
   * From
   * http://android-developers.blogspot.fr/2009/01/can-i-use-this-intent.html
   *
   * @param context The application's environment.
   * @param intent  The Intent action to check for availability.
   * @return True if an Intent with the specified action can be sent and
   * responded to, false otherwise.
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY);
    return !list.isEmpty();
  }

  public static boolean isIntentReceiverAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent, 0);
    return !list.isEmpty();
  }

  public static boolean isBrightColor(int color) {
    if (android.R.color.transparent == color)
      return true;

    boolean rtnValue = false;

    int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

    int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
        * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

    // color is light
    if (brightness >= 200) {
      rtnValue = true;
    }

    return rtnValue;
  }

  /**
   * get a value from extras that could be either passed as String or a long extra
   * we need this method, to pass values from monkeyrunner, which is not able to pass long extras
   * if extras is null, defaultValue is returned
   *
   * @param extras
   * @param key
   * @param defaultValue
   * @return
   */
  public static long getFromExtra(Bundle extras, String key, long defaultValue) {
    if (extras == null) return defaultValue;
    String stringValue = extras.getString(key);
    if (TextUtils.isEmpty(stringValue)) {
      return extras.getLong(key, defaultValue);
    } else {
      return Long.parseLong(stringValue);
    }
  }

  public static int get700Tint(int color) {
    int found = colorPrimaryDarkMap.get(color);
    return found != 0 ? found : color;
  }

  @SuppressLint("DefaultLocale")
  public static String toLocalizedString(int i) {
    return String.format("%d", i);
  }

  @VisibleForTesting
  public static CharSequence getContribFeatureLabelsAsFormattedList(
      Context ctx, ContribFeature other) {
    return getContribFeatureLabelsAsFormattedList(ctx, other, LicenceHandler.LicenceStatus.CONTRIB);
  }

  /**
   * @param ctx   for retrieving resources
   * @param other if not null, all features except the one provided will be returned
   * @param type  if not null, only features of this type will be listed
   * @return construct a list of all contrib features to be included into a
   * TextView
   */
  public static CharSequence getContribFeatureLabelsAsFormattedList(
      Context ctx, ContribFeature other, LicenceHandler.LicenceStatus type) {
    CharSequence result = "", linefeed = Html.fromHtml("<br>");
    for (ContribFeature f : EnumSet.allOf(ContribFeature.class)) {
      if (!f.equals(other) &&
          (!f.equals(ContribFeature.AD_FREE) || !DistribHelper.isGithub())) {
        if (type != null &&
            ((f.isExtended() && !type.equals(LicenceHandler.LicenceStatus.EXTENDED)) ||
                (!f.isExtended() && type.equals(LicenceHandler.LicenceStatus.EXTENDED)))) {
          continue;
        }
        String resName = "contrib_feature_" + f.toString() + "_label";
        int resId = ctx.getResources().getIdentifier(
            resName, "string",
            ctx.getPackageName());
        if (resId == 0) {
          AcraHelper.report(new Resources.NotFoundException(resName));
          continue;
        }
        if (!result.equals("")) {
          result = TextUtils.concat(result, linefeed);
        }
        result = TextUtils.concat(
            result,
            "\u25b6 ",
            ctx.getText(resId));
      }
    }
    return result;
  }

  public static String[] getContribFeatureLabelsAsList(Context ctx, LicenceHandler.LicenceStatus type) {
    Stream<ContribFeature> features = Stream.of(EnumSet.allOf(ContribFeature.class));
    if (type != null) {
      features =  features.filter(feature -> type.equals(LicenceHandler.LicenceStatus.CONTRIB) != feature.isExtended());
    }
    if (DistribHelper.isGithub()) {
      features = features.filter(feature -> !feature.equals(ContribFeature.AD_FREE));
    }
    return features
        .map(feature -> {
          String resName = "contrib_feature_" + feature.toString() + "_label";
          int resId = ctx.getResources().getIdentifier(
              resName, "string",
              ctx.getPackageName());
          return ctx.getText(resId);
        })
        .toArray(size -> new String[size]);
  }

  public static String md5(String s) {
    try {
      // Create MD5 Hash
      MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
      digest.update(s.getBytes());
      byte messageDigest[] = digest.digest();

      // Create Hex String
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++)
        hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static class StringBuilderWrapper {
    public StringBuilderWrapper() {
      this.sb = new StringBuilder();
    }

    private StringBuilder sb;

    public StringBuilderWrapper append(String s) {
      sb.append(s);
      return this;
    }

    public StringBuilderWrapper appendQ(String s) {
      sb.append(s.replace("\"", "\"\""));
      return this;
    }

    public String toString() {
      return sb.toString();
    }

    public void clear() {
      sb = new StringBuilder();
    }
  }

  /**
   * Credit:
   * https://groups.google.com/forum/?fromgroups#!topic/actionbarsherlock
   * /Z8Ic8djq-3o
   *
   * @param item
   * @param enabled
   */
  public static void menuItemSetEnabledAndVisible(MenuItem item, boolean enabled) {
    item.setEnabled(enabled).setVisible(enabled);
  }

  public static boolean doesPackageExist(Context context, String targetPackage) {
    try {
      context.getPackageManager().getPackageInfo(targetPackage,
          PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      return false;
    }
    return true;
  }

  public static DateFormat localizedYearlessDateFormat() {
    Locale l = Locale.getDefault();
    final String contextPattern = ((SimpleDateFormat) android.text.format.DateFormat.getDateFormat(
        MyApplication.getInstance())).toPattern();
    String yearlessPattern = contextPattern.replaceAll("\\W?[Yy]+\\W?", "");
    return new SimpleDateFormat(yearlessPattern, l);
  }

  public static Result analyzeGrisbiFileWithSAX(InputStream is) {
    GrisbiHandler handler = new GrisbiHandler();
    try {
      Xml.parse(is, Xml.Encoding.UTF_8, handler);
    } catch (IOException e) {
      return new Result(false, R.string.parse_error_other_exception,
          e.getMessage());
    } catch (GrisbiHandler.FileVersionNotSupportedException e) {
      return new Result(false,
          R.string.parse_error_grisbi_version_not_supported, e.getMessage());
    } catch (SAXException e) {
      return new Result(false, R.string.parse_error_parse_exception);
    }
    return handler.getResult();
  }

  public static int importParties(ArrayList<String> partiesList,
                                  GrisbiImportTask task) {
    int total = 0;
    for (int i = 0; i < partiesList.size(); i++) {
      if (Payee.maybeWrite(partiesList.get(i)) != -1) {
        total++;
      }
      if (task != null && i % 10 == 0) {
        task.publishProgress(i);
      }
    }
    return total;
  }

  public static int importCats(CategoryTree catTree, GrisbiImportTask task) {
    int count = 0, total = 0;
    String label;
    long main_id, sub_id;

    int size = catTree.children().size();
    for (int i = 0; i < size; i++) {
      CategoryTree mainCat = catTree.children().valueAt(i);
      label = mainCat.getLabel();
      count++;
      main_id = Category.find(label, null);
      if (main_id != -1) {
        Timber.i("category with label %s already defined", label);
      } else {
        main_id = Category.write(0L, label, null);
        if (main_id != -1) {
          total++;
          if (task != null && count % 10 == 0) {
            task.publishProgress(count);
          }
        } else {
          // this should not happen
          Timber.w("could neither retrieve nor store main category %s", label);
          continue;
        }
      }
      int subSize = mainCat.children().size();
      for (int j = 0; j < subSize; j++) {
        label = mainCat.children().valueAt(j).getLabel();
        count++;
        sub_id = Category.write(0L, label, main_id);
        if (sub_id != -1) {
          total++;
        } else {
          Timber.i("could not store sub category %s", label);
        }
        if (task != null && count % 10 == 0) {
          task.publishProgress(count);
        }
      }
    }
    return total;
  }


  public static String concatResStrings(Context ctx, String separator, Integer... resIds) {
    String result = "";
    Iterator<Integer> itemIterator = Arrays.asList(resIds).iterator();
    if (itemIterator.hasNext()) {
      result += ctx.getString(itemIterator.next());
      while (itemIterator.hasNext()) {
        result += separator + ctx.getString(itemIterator.next());
      }
    }
    return result;
  }

  // From Financisto
  public static String[] joinArrays(String[] a1, String[] a2) {
    if (a1 == null || a1.length == 0) {
      return a2;
    }
    if (a2 == null || a2.length == 0) {
      return a1;
    }
    String[] a = new String[a1.length + a2.length];
    System.arraycopy(a1, 0, a, 0, a1.length);
    System.arraycopy(a2, 0, a, a1.length, a2.length);
    return a;
  }

  public static void configDecimalSeparator(final EditText editText,
                                            final char decimalSeparator, final int fractionDigits) {
    // mAmountText.setInputType(
    // InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
    // due to bug in Android platform
    // http://code.google.com/p/android/issues/detail?id=2626
    // the soft keyboard if it occupies full screen in horizontal orientation
    // does not display the , as comma separator
    // TODO we should take into account the arab separator as well
    final char otherSeparator = decimalSeparator == '.' ? ',' : '.';
    editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    editText.setFilters(new InputFilter[]{new InputFilter() {
      @Override
      public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                                 int dend) {
        int separatorPositionInDest = dest.toString().indexOf(decimalSeparator);
        char[] v = new char[end - start];
        TextUtils.getChars(source, start, end, v, 0);
        String input = new String(v).replace(otherSeparator, decimalSeparator);
        if (fractionDigits == 0 || separatorPositionInDest != -1 || dest.length() - dend > fractionDigits) {
          input = input.replace(String.valueOf(decimalSeparator), "");
        } else {
          int separatorPositionInSource = input.lastIndexOf(decimalSeparator);
          if (separatorPositionInSource != -1) {
            //we make sure there is only one separator in the input and after the separator we do not use
            //more minor digits as allowed
            int existingMinorUnits = dest.length() - dend;
            int additionalAllowedMinorUnits = fractionDigits - existingMinorUnits;
            int additionalPossibleMinorUnits = input.length() - separatorPositionInSource - 1;
            int extractMinorUnits = additionalPossibleMinorUnits >= additionalAllowedMinorUnits ?
                additionalAllowedMinorUnits : additionalPossibleMinorUnits;
            input = input.substring(0, separatorPositionInSource).replace(String.valueOf
                (decimalSeparator), "") +
                decimalSeparator + (extractMinorUnits > 0 ?
                input.substring(separatorPositionInSource + 1,
                    separatorPositionInSource + 1 + extractMinorUnits) :
                "");
          }
        }
        if (fractionDigits == 0) {
          return input;
        }
        if (separatorPositionInDest != -1 &&
            dend > separatorPositionInDest && dstart > separatorPositionInDest) {
          int existingMinorUnits = dest.length() - (separatorPositionInDest + 1);
          int remainingMinorUnits = fractionDigits - existingMinorUnits;
          if (remainingMinorUnits < 1) {
            return "";
          }
          return input.length() > remainingMinorUnits ? input.substring(0, remainingMinorUnits) :
              input;
        } else {
          return input;
        }
      }
    }, new InputFilter.LengthFilter(16)});
  }

  /**
   * @param str
   * @return a representation of str converted to lower case, Unicode
   * normalization applied and markers removed this allows
   * case-insentive comparison for non-ascii and non-latin strings
   */
  public static String normalize(String str) {
    str = str.toLowerCase();
    // Credits: http://stackoverflow.com/a/3322174/1199911
    return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{M}",
        "");
  }

  public static String esacapeSqlLikeExpression(String str) {
    return str
        .replace(WhereFilter.LIKE_ESCAPE_CHAR,
            WhereFilter.LIKE_ESCAPE_CHAR + WhereFilter.LIKE_ESCAPE_CHAR)
        .replace("%", WhereFilter.LIKE_ESCAPE_CHAR + "%")
        .replace("_", WhereFilter.LIKE_ESCAPE_CHAR + "_");
  }

  public static String printDebug(Object[] objects) {
    if (objects == null) {
      return "null";
    }
    String result = "";
    for (Object object : objects) {
      if (!result.equals(""))
        result += ",";
      result += (object == null ? "null" : object.toString());
    }
    return result;
  }

  public static Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth,
                                                  int reqHeight) {

    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;

    if (uri.getScheme().equals("file")) {
      String filePath = uri.getPath();
      BitmapFactory.decodeFile(filePath, options);

      // Calculate inSampleSize
      options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

      // Decode bitmap with inSampleSize set
      options.inJustDecodeBounds = false;
      return BitmapFactory.decodeFile(filePath, options);
    } else {
      InputStream is = null;
      try {
        is = MyApplication.getInstance().getContentResolver()
            .openInputStream(uri);
        BitmapFactory.decodeStream(is, null, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
            reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        is.close();
        is = MyApplication.getInstance().getContentResolver()
            .openInputStream(uri);
        return BitmapFactory.decodeStream(is, null, options);
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
    return null;
  }

  public static int calculateInSampleSize(BitmapFactory.Options options,
                                          int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps
      // both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight
          && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  /**
   * filters out the '/' character and characters of type {@link java.lang.Character#SURROGATE} or
   * {@link java.lang.Character#OTHER_SYMBOL}, meant primarily to skip emojs
   *
   * @param in
   * @return
   */
  public static String escapeForFileName(String in) {
    return in.replace("/", "").replaceAll("\\p{Cs}", "").replaceAll("\\p{So}", "");
  }

  //http://stackoverflow.com/a/11072627/1199911
  public static void selectSpinnerItemByValue(Spinner spnr, long value) {
    SimpleCursorAdapter adapter = (SimpleCursorAdapter) spnr.getAdapter();
    for (int position = 0; position < adapter.getCount(); position++) {
      if (adapter.getItemId(position) == value) {
        spnr.setSelection(position);
        return;
      }
    }
  }

  @SuppressLint("NewApi")
  public static void setBackgroundTintListOnFab(FloatingActionButton fab, int color) {
    fab.setBackgroundTintList(ColorStateList.valueOf(color));
    DrawableCompat.setTint(fab.getDrawable(), isBrightColor(color) ? Color.BLACK : Color.WHITE);
    fab.invalidate();
  }

  public static int getFirstDayOfWeek(Locale locale) {
    return new GregorianCalendar(locale).getFirstDayOfWeek();
  }

  public static int getFirstDayOfWeekFromPreferenceWithFallbackToLocale(Locale locale) {
    String weekStartsOn = PrefKey.GROUP_WEEK_STARTS.getString("-1");
    return weekStartsOn.equals("-1") ? Utils.getFirstDayOfWeek(locale) :
        Integer.parseInt(weekStartsOn);
  }

  public static void configureSortMenu(SubMenu sortMenu, String currentSortOrder) {
    MenuItem activeItem;
    switch (currentSortOrder) {
      case ProtectedFragmentActivity.SORT_ORDER_USAGES:
        activeItem = sortMenu.findItem(R.id.SORT_USAGES_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_LAST_USED:
        activeItem = sortMenu.findItem(R.id.SORT_LAST_USED_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_AMOUNT:
        activeItem = sortMenu.findItem(R.id.SORT_AMOUNT_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_CUSTOM:
        activeItem = sortMenu.findItem(R.id.SORT_CUSTOM_COMMAND);
        break;
      case ProtectedFragmentActivity.SORT_ORDER_NEXT_INSTANCE:
        activeItem = sortMenu.findItem(R.id.SORT_NEXT_INSTANCE_COMMAND);
        break;
      default:
        activeItem = sortMenu.findItem(R.id.SORT_TITLE_COMMAND);
    }
    activeItem.setChecked(true);
  }

  public static String getSortOrderFromMenuItemId(int id) {
    switch (id) {
      case R.id.SORT_USAGES_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_USAGES;
      case R.id.SORT_LAST_USED_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_LAST_USED;
      case R.id.SORT_TITLE_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_TITLE;
      case R.id.SORT_CUSTOM_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_CUSTOM;
      case R.id.SORT_AMOUNT_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_AMOUNT;
      case R.id.SORT_NEXT_INSTANCE_COMMAND:
        return ProtectedFragmentActivity.SORT_ORDER_NEXT_INSTANCE;
    }
    return null;
  }

  public static void configureGroupingMenu(SubMenu groupingMenu, Grouping currentGrouping) {
    MenuItem activeItem;
    switch (currentGrouping) {
      case DAY:
        activeItem = groupingMenu.findItem(R.id.GROUPING_DAY_COMMAND);
        break;
      case WEEK:
        activeItem = groupingMenu.findItem(R.id.GROUPING_WEEK_COMMAND);
        break;
      case MONTH:
        activeItem = groupingMenu.findItem(R.id.GROUPING_MONTH_COMMAND);
        break;
      case YEAR:
        activeItem = groupingMenu.findItem(R.id.GROUPING_YEAR_COMMAND);
        break;
      default:
        activeItem = groupingMenu.findItem(R.id.GROUPING_NONE_COMMAND);
        break;
    }
    activeItem.setChecked(true);
  }

  public static Grouping getGroupingFromMenuItemId(int id) {
    switch (id) {
      case R.id.GROUPING_NONE_COMMAND:
        return Grouping.NONE;
      case R.id.GROUPING_DAY_COMMAND:
        return Grouping.DAY;
      case R.id.GROUPING_WEEK_COMMAND:
        return Grouping.WEEK;
      case R.id.GROUPING_MONTH_COMMAND:
        return Grouping.MONTH;
      case R.id.GROUPING_YEAR_COMMAND:
        return Grouping.YEAR;
    }
    return null;
  }

  public static Bitmap getTintedBitmapForTheme(Context context, int drawableResId, int themeResId) {
    Drawable d = getTintedDrawableForTheme(context, drawableResId, themeResId);
    return drawableToBitmap(d);
  }

  private static Drawable getTintedDrawableForTheme(Context context, int drawableResId, int themeResId) {
    Context wrappedContext = new ContextThemeWrapper(context, themeResId);
    //noinspection RestrictedApi
    return AppCompatDrawableManager.get().getDrawable(wrappedContext, drawableResId);
  }

  private static Bitmap drawableToBitmap(Drawable d) {
    Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(),
        d.getIntrinsicHeight(),
        Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    d.setBounds(0, 0, c.getWidth(), c.getHeight());
    d.draw(c);
    return b;
  }

  public static void requireLoader(LoaderManager manager, int loaderId, Bundle args,
                                   LoaderManager.LoaderCallbacks callback) {
    if (manager.getLoader(loaderId) != null && !manager.getLoader(loaderId).isReset()) {
      manager.restartLoader(loaderId, args, callback);
    } else {
      manager.initLoader(loaderId, args, callback);
    }
  }

  //Integer compare is API 19
  public static int compare(int lhs, int rhs) {
    return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
  }

  // From Guava
  public static int indexOf(int[] array, int target) {
    return indexOf(array, target, 0, array.length);
  }

  private static int indexOf(
      int[] array, int target, int start, int end) {
    for (int i = start; i < end; i++) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  public static int pow(int b, int k) {
    switch (b) {
      case 0:
        return (k == 0) ? 1 : 0;
      case 1:
        return 1;
      case (-1):
        return ((k & 1) == 0) ? 1 : -1;
      case 2:
        return (k < Integer.SIZE) ? (1 << k) : 0;
      case (-2):
        if (k < Integer.SIZE) {
          return ((k & 1) == 0) ? (1 << k) : -(1 << k);
        } else {
          return 0;
        }
      default:
        // continue below to handle the general case
    }
    for (int accum = 1; ; k >>= 1) {
      switch (k) {
        case 0:
          return accum;
        case 1:
          return b * accum;
        default:
          accum *= ((k & 1) == 0) ? 1 : b;
          b *= b;
      }
    }
  }

  public static CharSequence makeBulletList(Context ctx, String... lines) {
    InsetDrawable drawable = new InsetDrawable(
        Utils.getTintedDrawableForTheme(ctx, R.drawable.ic_menu_done, R.style.ThemeDark), 0, 20, 0, 0);
    Bitmap bitmap = drawableToBitmap(drawable);
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.5),
        (int) (bitmap.getHeight() * 0.5), true);
    SpannableStringBuilder sb = new SpannableStringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      Spannable spannable = new SpannableString(line + (i < lines.length - 1 ? "\n" : ""));
      spannable.setSpan(new IconMarginSpan(scaledBitmap, 25), 0, line.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
      sb.append(spannable);
    }
    return sb;
  }

}
