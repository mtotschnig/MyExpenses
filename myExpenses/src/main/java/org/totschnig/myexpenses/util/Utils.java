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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

//import org.acra.ErrorReporter;
import com.google.common.annotations.VisibleForTesting;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.task.GrisbiImportTask;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ChoiceFormat;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES;

/**
 * Util class with helper methods
 * 
 * @author Michael Totschnig
 * 
 */
public class Utils {

  public static final boolean IS_FLAVOURED = !BuildConfig.FLAVOR.equals("");
  private static NumberFormat numberFormat;

  private static void initNumberFormat() {
    String prefFormat = MyApplication.PrefKey.CUSTOM_DECIMAL_FORMAT.getString("");
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
    if (numberFormat==null) {
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

  public static String defaultOrderBy(String textColumn) {
    boolean byUsagesP = MyApplication.PrefKey.SORT_ORDER.getString("USAGES").equals("USAGES");
    return (byUsagesP ? KEY_USAGES + " DESC, " : "")
        + textColumn + " COLLATE LOCALIZED";
  }

  /**
   * <a href="http://www.ibm.com/developerworks/java/library/j-numberformat/">
   * http://www.ibm.com/developerworks/java/library/j-numberformat/</a>
   * 
   * @param strFloat
   *          parsed as float with the number format defined in the locale
   * @return the float retrieved from the string or null if parse did not
   *         succeed
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

  public static URI validateUri(String target) {
    boolean targetParsable;
    URI uri = null;
    if (!target.equals("")) {
      try {
        uri = new URI(target);
        String scheme = uri.getScheme();
        // strangely for mailto URIs getHost returns null,
        // so we make sure that mailto URIs handled as valid
        targetParsable = scheme != null
            && (scheme.equals("mailto") || uri.getHost() != null);
      } catch (URISyntaxException e1) {
        targetParsable = false;
      }
      if (!targetParsable) {
        return null;
      }
      return uri;
    }
    return null;
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

  static String formatCurrency(BigDecimal amount, Currency currency) {
    NumberFormat nf = getNumberFormat();
    int fractionDigits = Money.fractionDigits(currency);
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
   *         currency, and with the given separator, but without the currency
   *         symbol appropriate for CSV and QIF export
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
   * @param text
   *          unixEpochAsString
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
   * @param text
   *          amount as String
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
      Log.e("MyExpenses", strCurrency + " is not defined in ISO 4217");
      c = Currency.getInstance(Locale.getDefault());
    }
    return getSaveInstance(c);
  }

  public static Currency getSaveInstance(Currency currency) {
    try {
      Account.CurrencyEnum.valueOf(currency.getCurrencyCode());
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

  /**
   * @return the directory user has configured in the settings, if not configured yet
   * returns {@link android.content.ContextWrapper#getExternalFilesDir(String)} with argument null
   */
  public static DocumentFile getAppDir() {
    String prefString = MyApplication.PrefKey.APP_DIR.getString(null);
    if (prefString != null) {
      Uri pref = Uri.parse(prefString);
      if (pref.getScheme().equals("file")) {
        File appDir = new File(pref.getPath());
        if (appDir.mkdir() || appDir.isDirectory()) {
          return DocumentFile.fromFile(appDir);
        }/* else {
          Utils.reportToAcra(new Exception("Found invalid preference value " + prefString));
        }*/
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          //this will return null, if called on a pre-Lolipop device
          return DocumentFile.fromTreeUri(MyApplication.getInstance(), pref);
        }
      }
    }
    File externalFilesDir = MyApplication.getInstance().getExternalFilesDir(null);
    if (externalFilesDir != null) {
      return DocumentFile.fromFile(externalFilesDir);
    } else {
      Utils.reportToAcra(new Exception("getExternalFilesDir returned null"));
      return null;
    }
  }

  public static File getCacheDir() {
    File external = MyApplication.getInstance().getExternalCacheDir();
    return external != null ? external : MyApplication.getInstance()
        .getCacheDir();
  }

  /**
   * @param parentDir
   * @param prefix
   * @param addExtension
   * @return creates a file object in parentDir, with a timestamp appended to
   *         prefix as name, if the file already exists it appends a numeric
   *         postfix
   */
  public static DocumentFile timeStampedFile(DocumentFile parentDir, String prefix,
                                             String mimeType, boolean addExtension) {
    String now = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        .format(new Date());
    return newFile(parentDir,prefix + "-" + now,mimeType,addExtension);
  }

  public static DocumentFile newFile(DocumentFile parentDir, String base,
                                             String mimeType, boolean addExtension) {
    int postfix = 0;
    do {
      String name = base;
      if (postfix>0) {
        name += "_"+postfix;
      }
      if (addExtension) {
        name+="."+mimeType.split("/")[1];
      }
      if (parentDir.findFile(name)==null) {
        DocumentFile result = null;
        try {
          result = parentDir.createFile(mimeType, name);
          if (result == null) {
            Utils.reportToAcra(new Exception(String.format(
                "createFile returned null: mimeType %s; name %s; parent %s",
                mimeType,name,parentDir.getUri().toString())));
          }
        } catch (SecurityException e) {
          Utils.reportToAcra(new Exception(String.format(
              "createFile threw SecurityException: mimeType %s; name %s; parent %s",
              mimeType, name, parentDir.getUri().toString())));
        }
        return result;
      }
      postfix++;
    } while (true);
  }


  public static DocumentFile newDirectory(DocumentFile parentDir, String base) {
    int postfix = 0;
    do {
      String name = base;
      if (postfix>0) {
        name += "_"+postfix;
      }
      if (parentDir.findFile(name)==null) {
        return parentDir.createDirectory(name);
      }
      postfix++;
    } while (true);
  }

  /**
   * Helper Method to Test if external Storage is Available from
   * http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html
   */
  public static boolean isExternalStorageAvailable() {
    boolean state = false;
    String extStorageState = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
      state = true;
    }
    return state;
  }

  public static Result checkAppDir() {
    if (!Utils.isExternalStorageAvailable()) {
      return new Result(false, R.string.external_storage_unavailable);
    }
    DocumentFile appDir = getAppDir();
    if (appDir == null) {
      return new Result(false, R.string.io_error_appdir_null);
    }
    return dirExistsAndIsWritable(appDir) ?
        new Result(true) : new Result(false, R.string.app_dir_not_accessible,
        FileUtils.getPath(MyApplication.getInstance(), appDir.getUri()));
  }

  @NonNull
  public static boolean dirExistsAndIsWritable(DocumentFile appdir) {
    return appdir.exists() && appdir.canWrite();
  }

  public static boolean copy(File src, File dst) {
    FileInputStream srcStream = null;
    FileOutputStream dstStream = null;
    try {
      srcStream = new FileInputStream(src);
      dstStream = new FileOutputStream(dst);
      dstStream.getChannel().transferFrom(srcStream.getChannel(), 0,
          srcStream.getChannel().size());
      return true;
    } catch (FileNotFoundException e) {
      Log.e("MyExpenses", e.getLocalizedMessage());
      return false;
    } catch (IOException e) {
      Log.e("MyExpenses", e.getLocalizedMessage());
      return false;
    } finally {
      try {
        srcStream.close();
      } catch (Exception e) {
      }
      try {
        dstStream.close();
      } catch (Exception e) {
      }
    }
  }

  /** Create a File for saving an image or video */
  // Source
  // http://developer.android.com/guide/topics/media/camera.html#saving-media

  /**
   * create a File object for storage of picture data
   * 
   * @param temp
   *          if true the returned file is suitable for temporary storage while
   *          the user is editing the transaction if false the file will serve
   *          as permanent storage,
   *          care is taken that the file does not yet exist
   * @return a file on the external storage
   */
  public static File getOutputMediaFile(String fileName, boolean temp) {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = temp ? getCacheDir() : getPictureDir();
    if (mediaStorageDir==null) return null;
    int postfix = 0;
    File result;
    do {
      result = new File(mediaStorageDir, getOutputMediaFileName(
          fileName,
          postfix));
      postfix++;
    } while (result.exists());
    return result;
  }
  public static Uri getOutputMediaUri(boolean temp) {
    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(new Date());
    File outputMediaFile;
    if (MyApplication.getInstance().isProtected() && !temp) {
      outputMediaFile = getOutputMediaFile(fileName, false);
      if (outputMediaFile==null) return null;
      return FileProvider.getUriForFile(MyApplication.getInstance(),
                 "org.totschnig.myexpenses.fileprovider",
          outputMediaFile);
    } else {
      outputMediaFile = getOutputMediaFile(fileName,temp);
      if (outputMediaFile==null) return null;
      return Uri.fromFile(outputMediaFile);
    }
  }

  public static String getPictureUriBase(boolean temp) {
    Uri sampleUri = getOutputMediaUri(temp);
    if (sampleUri==null) return null;
    String uriString = sampleUri.toString();
    return uriString.substring(0,uriString.lastIndexOf('/'));
  }

  private static String getOutputMediaFileName(String base,int postfix) {
      if (postfix>0) {
        base+= "_"+postfix;
      }
      return base + ".jpg";
  }
    public static File getPictureDir() {
        return getPictureDir(MyApplication.getInstance().isProtected());
    }
  public static File getPictureDir(boolean secure) {
    File result;
    if (secure) {
      result = new File (MyApplication.getInstance().getFilesDir(),
          "images");
    } else {
      result = MyApplication.getInstance().getExternalFilesDir(
          Environment.DIRECTORY_PICTURES);
    }
    if (result==null) return null;
    result.mkdir();
    return result.exists() ? result : null;
  }

  /**
   * copy src uri to dest uri
   * 
   * @param src
   * @param dest
   * @return
   */
  public static void copy(Uri src, Uri dest) throws IOException {
    InputStream input = null;
    OutputStream output = null;

    try {
      input = MyApplication.getInstance().getContentResolver()
          .openInputStream(src);
      if (input==null) {
        throw new IOException("Could not open InputStream "+src.toString());
      }
      output = MyApplication.getInstance().getContentResolver()
              .openOutputStream(dest);
      if (output==null) {
        throw new IOException("Could not open OutputStream "+dest.toString());
      }
      final byte[] buffer = new byte[1024];
      int read;

      while ((read = input.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
      output.flush();
    } finally {
      try {
        if (input!=null) input.close();
      } catch (IOException e) {
      }
      try {
        if (output!=null) output.close();
      } catch (IOException e) {
      }
    }
  }

  public static void share(Context ctx, ArrayList<Uri> fileUris, String target,
      String mimeType) {
    URI uri = null;
    Intent intent;
    String scheme = "mailto";
    boolean multiple = fileUris.size() > 1;
    if (!target.equals("")) {
      uri = Utils.validateUri(target);
      if (uri == null) {
        Toast.makeText(ctx, ctx.getString(R.string.ftp_uri_malformed, target),
            Toast.LENGTH_LONG).show();
        return;
      }
      scheme = uri.getScheme();
    }
    // if we get a String that does not include a scheme,
    // we interpret it as a mail address
    if (scheme == null) {
      scheme = "mailto";
    }
    if (scheme.equals("ftp")) {
      if (multiple) {
        Toast.makeText(ctx,
            "sending multiple file through ftp is not supported",
            Toast.LENGTH_LONG).show();
        return;
      }
      intent = new Intent(android.content.Intent.ACTION_SENDTO);
      intent.putExtra(Intent.EXTRA_STREAM, fileUris.get(0));
      intent.setDataAndType(android.net.Uri.parse(target), mimeType);
      if (!isIntentAvailable(ctx, intent)) {
        Toast.makeText(ctx, R.string.no_app_handling_ftp_available,
            Toast.LENGTH_LONG).show();
        return;
      }
      ctx.startActivity(intent);
    } else if (scheme.equals("mailto")) {
      if (multiple) {
        intent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (Uri fileUri : fileUris) {
          uris.add(fileUri);
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
      } else {
        intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, fileUris.get(0));
      }
      intent.setType(mimeType);
      if (uri != null) {
        String address = uri.getSchemeSpecificPart();
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
      }
      intent.putExtra(Intent.EXTRA_SUBJECT, R.string.export_expenses);
      if (!isIntentAvailable(ctx, intent)) {
        Toast.makeText(ctx, R.string.no_app_handling_email_available,
            Toast.LENGTH_LONG).show();
        return;
      }
      // if we got mail address, we launch the default application
      // if we are called without target, we launch the chooser
      // in order to make action more explicit
      if (uri != null) {
        ctx.startActivity(intent);
      } else {
        ctx.startActivity(Intent.createChooser(intent,
            ctx.getString(R.string.share_sending)));
      }
    } else {
      Toast.makeText(ctx,
          ctx.getString(R.string.share_scheme_not_supported, scheme),
          Toast.LENGTH_LONG).show();
      return;
    }
  }

  public static void setBackgroundFilter(View v, int c) {
    v.getBackground().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
  }

  /**
   * Indicates whether the specified action can be used as an intent. This
   * method queries the package manager for installed packages that can respond
   * to an intent with the specified action. If no suitable package is found,
   * this method returns false.
   * 
   * From
   * http://android-developers.blogspot.fr/2009/01/can-i-use-this-intent.html
   * 
   * @param context
   *          The application's environment.
   * @param intent
   *          The Intent action to check for availability.
   * 
   * @return True if an Intent with the specified action can be sent and
   *         responded to, false otherwise.
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY);
    return list.size() > 0;
  }

  public static boolean isIntentReceiverAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent, 0);
    return list.size() > 0;
  }

  public static boolean isBrightColor(int color) {
    if (android.R.color.transparent == color)
      return true;

    boolean rtnValue = false;

    int[] rgb = { Color.red(color), Color.green(color), Color.blue(color) };

    int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
        * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

    // color is light
    if (brightness >= 200) {
      rtnValue = true;
    }

    return rtnValue;
  }

  /**
   *
   * @param key
   * @return
   */
  public static LicenceStatus verifyLicenceKey(String key) {
    String secret= MyApplication.CONTRIB_SECRET;
    String extendedSecret = secret+"_EXTENDED";
    String androidId = Settings.Secure.getString(MyApplication.getInstance()
        .getContentResolver(), Settings.Secure.ANDROID_ID);
    String s = androidId + extendedSecret;
    Long l = (s.hashCode() & 0x00000000ffffffffL);
    if (l.toString().equals(key)) {
      return LicenceStatus.EXTENDED;
    }
    s = androidId + secret;
    l = (s.hashCode() & 0x00000000ffffffffL);
    return l.toString().equals(key) ? LicenceStatus.CONTRIB : null;
  }

  /**
   * get a value from extras that could be either passed as String or a long extra
   * we need this method, to pass values from monkeyrunner, which is not able to pass long extras
   * @param extras
   * @param key
   * @param defaultValue
   * @return
   */
  public static long getFromExtra(Bundle extras, String key, long defaultValue) {
    String stringValue = extras.getString(key);
    if (TextUtils.isEmpty(stringValue)) {
      return extras.getLong(key,defaultValue);
    } else {
      return Long.parseLong(stringValue);
    }
  }

  public enum LicenceStatus {
    CONTRIB, EXTENDED
  }

  @VisibleForTesting
  public static CharSequence getContribFeatureLabelsAsFormattedList(
      Context ctx, ContribFeature other) {
    return getContribFeatureLabelsAsFormattedList(ctx,other,LicenceStatus.CONTRIB);
  }
  /**
   * @param ctx
   *          for retrieving resources
   * @param other
   *          if not null, all features except the one provided will be returned
   * @param type if not null, only features of this type will be listed
   * @return construct a list of all contrib features to be included into a
   *         TextView
   */
  public static CharSequence getContribFeatureLabelsAsFormattedList(
      Context ctx, ContribFeature other, LicenceStatus type) {
    CharSequence result = "", linefeed = Html.fromHtml("<br>");
    Iterator<ContribFeature> iterator = EnumSet.allOf(ContribFeature.class)
        .iterator();
    while (iterator.hasNext()) {
      ContribFeature f = iterator.next();
      if (!f.equals(other) &&
          (!f.equals(ContribFeature.AD_FREE) || IS_FLAVOURED)) {
        if (type !=null &&
            ((f.isExtended && !type.equals(LicenceStatus.EXTENDED)) ||
            (!f.isExtended && type.equals(LicenceStatus.EXTENDED)))) {
          continue;
        }
        String resName = "contrib_feature_" + f.toString() + "_label";
        int resId = ctx.getResources().getIdentifier(
            resName, "string",
            ctx.getPackageName());
        if (resId==0) {
          reportToAcra(new Resources.NotFoundException(resName));
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

  public static <E extends Enum<E>> String joinEnum(Class<E> enumClass) {
    String result = "";
    Iterator<E> iterator = EnumSet.allOf(enumClass).iterator();
    while (iterator.hasNext()) {
      result += "'" + iterator.next().name() + "'";
      if (iterator.hasNext())
        result += ",";
    }
    return result;
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

    for (Map.Entry<Integer, CategoryTree> main : catTree.children().entrySet()) {
      CategoryTree mainCat = main.getValue();
      label = mainCat.getLabel();
      count++;
      main_id = Category.find(label, null);
      if (main_id != -1) {
        Log.i("MyExpenses", "category with label" + label + " already defined");
      } else {
        main_id = Category.write(0L, label, null);
        if (main_id != -1) {
          total++;
          if (task != null && count % 10 == 0) {
            task.publishProgress(count);
          }
        } else {
          // this should not happen
          Log.w("MyExpenses", "could neither retrieve nor store main category "
              + label);
          continue;
        }
      }
      for (Map.Entry<Integer, CategoryTree> sub : mainCat.children().entrySet()) {
        label = sub.getValue().getLabel();
        count++;
        sub_id = Category.write(0L, label, main_id);
        if (sub_id != -1) {
          total++;
        } else {
          Log.i("MyExpenses", "could not store sub category " + label);
        }
        if (task != null && count % 10 == 0) {
          task.publishProgress(count);
        }
      }
    }
    return total;
  }

  public static void reportToAcraWithDbSchema(Exception e) {
    // reportToAcra(e, "DB_SCHEMA", DbUtils.getTableDetails());
    reportToAcra(e);
  }

  public static void reportToAcra(Exception e,String key,String data) {
    // ErrorReporter errorReporter = org.acra.ACRA.getErrorReporter();
    // errorReporter.putCustomData(key, data);
    // errorReporter.handleException(e);
    // errorReporter.removeCustomData(key);
    Log.e(MyApplication.TAG, key + ": " + data);
    reportToAcra(e);
  }

  public static void reportToAcra(Exception e) {
    Log.e(MyApplication.TAG, "Report", e);
    /* org.acra.ACRA.getErrorReporter().handleSilentException(e); */
  }

  public static String concatResStrings(Context ctx, Integer... resIds) {
    String result = "";
    Iterator<Integer> itemIterator = Arrays.asList(resIds).iterator();
    if (itemIterator.hasNext()) {
      result += ctx.getString(itemIterator.next());
      while (itemIterator.hasNext()) {
        result += " " + ctx.getString(itemIterator.next());
      }
    }
    return result;
  }

  /**
   * @return false if the configured folder is inside the application folder
   *         that will be deleted upon app uninstall and hence user should be
   *         warned about the situation, unless he already has opted to no
   *         longer see this warning
   */
  @SuppressLint("NewApi")
  public static boolean checkAppFolderWarning() {
    // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
    // return true;
    // }
    if (MyApplication.PrefKey.APP_FOLDER_WARNING_SHOWN.getBoolean(false)) {
      return true;
    }
    try {
      DocumentFile configuredDir = Utils.getAppDir();
      if (configuredDir == null) {
        return true;
      }
      File externalFilesDir = MyApplication.getInstance().getExternalFilesDir(
          null);
      if (externalFilesDir == null) {
        return true;
      }
      Uri dirUri = configuredDir.getUri();
      if (!dirUri.getScheme().equals("file")) {
        return true; //nothing we can do if we can not compare paths
      }
      URI defaultDir = externalFilesDir.getParentFile().getCanonicalFile()
          .toURI();
      return defaultDir.relativize(new File(dirUri.getPath()).getCanonicalFile().toURI())
          .isAbsolute();
    } catch (IOException e) {
      return true;
    }
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
      public CharSequence filter(CharSequence source, int start, int end,
                                 Spanned dest, int dstart, int dend) {
        int separatorPosition = dest.toString().indexOf(decimalSeparator);
        if (fractionDigits > 0) {
          int minorUnits = separatorPosition == -1 ? 0 : dest.length()
              - (separatorPosition + 1);
          if (dstart > separatorPosition && dend > separatorPosition) {
            // filter is only needed if we are past the separator
            // and the change increases length of string
            if (dend - dstart < end - start && minorUnits >= fractionDigits)
              return "";
          }
        }
        for (int i = start; i < end; i++) {
          if (source.charAt(i) == otherSeparator
              || source.charAt(i) == decimalSeparator) {
            char[] v = new char[end - start];
            TextUtils.getChars(source, start, end, v, 0);
            String s = new String(v).replace(otherSeparator, decimalSeparator);
            if (fractionDigits == 0 || // no separator allowed
                separatorPosition > -1 || // we already have a separator
                dest.length() - dend > fractionDigits) // the separator would be
              // positioned so that we have too many fraction digits
              return s.replace(String.valueOf(decimalSeparator), "");
            else
              return s;
          }
        }
        return null; // keep original
      }
    }, new InputFilter.LengthFilter(16)});
  }

  /**
   * @param str
   * @return a representation of str converted to lower case, Unicode
   *         normalization applied and markers removed this allows
   *         case-insentive comparison for non-ascii and non-latin strings works
   *         only above Gingerbread, on Froyo only lower case transformation is
   *         performed
   */
  @SuppressLint({ "NewApi", "DefaultLocale" })
  public static String normalize(String str) {
    str = str.toLowerCase();
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
      return str;
    }
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
      result += object.toString();
    }
    return result;
  }

  @SuppressLint("InlinedApi")
  public static String getContentIntentAction() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Intent.ACTION_OPEN_DOCUMENT
        : Intent.ACTION_GET_CONTENT;
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

  public static String escapeForFileName(String in) {
    return in.replace("/","");
  }

  //http://stackoverflow.com/a/11072627/1199911
  public static void selectSpinnerItemByValue(Spinner spnr, long value)
  {
    SimpleCursorAdapter adapter = (SimpleCursorAdapter) spnr.getAdapter();
    for (int position = 0; position < adapter.getCount(); position++)
    {
      if(adapter.getItemId(position) == value)
      {
        spnr.setSelection(position);
        return;
      }
    }
  }

  @SuppressLint("NewApi")
  public static void setBackgroundTintListOnFab(FloatingActionButton fab, int color) {
    fab.setBackgroundTintList(ColorStateList.valueOf(color));
  }
}
