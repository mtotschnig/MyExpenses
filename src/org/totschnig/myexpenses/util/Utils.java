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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.EnumSet;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.TransactionDatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Util class with helper methods
 * @author Michael Totschnig
 *
 */
public class Utils {

  public static char getDefaultDecimalSeparator() {
    char sep = '.';
    NumberFormat nfDLocal = NumberFormat.getNumberInstance();
    if (nfDLocal instanceof DecimalFormat) {
      DecimalFormatSymbols symbols = ((DecimalFormat)nfDLocal).getDecimalFormatSymbols();
      sep=symbols.getDecimalSeparator();
    }
    return sep;
  }
  
  /**
   * <a href="http://www.ibm.com/developerworks/java/library/j-numberformat/">http://www.ibm.com/developerworks/java/library/j-numberformat/</a>
   * @param strFloat parsed as float with the number format defined in the locale
   * @return the float retrieved from the string or null if parse did not succeed
   */
  public static BigDecimal validateNumber(DecimalFormat df, String strFloat) {
    ParsePosition pp;
    pp = new ParsePosition( 0 );
    pp.setIndex( 0 );
    df.setParseBigDecimal(true);
    BigDecimal n = (BigDecimal) df.parse(strFloat,pp);
    if( strFloat.length() != pp.getIndex() || 
        n == null )
    {
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
        //strangely for mailto URIs getHost returns null, so we make sure that mailto URIs handled as valid
        targetParsable = scheme != null && (scheme.equals("mailto") || uri.getHost() != null);
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
   * @param amount
   * @param currency
   * @return formated string
   */
  public static String formatCurrency(Money money) {
    BigDecimal amount = money.getAmountMajor();
    Currency currency = money.getCurrency();
    return formatCurrency(amount,currency);
  }
  static String formatCurrency(BigDecimal amount, Currency currency) {
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    int fractionDigits = currency.getDefaultFractionDigits();
    nf.setCurrency(currency);
    if (fractionDigits != -1) {
      nf.setMinimumFractionDigits(fractionDigits);
      nf.setMaximumFractionDigits(fractionDigits);
    } else {
      nf.setMaximumFractionDigits(Money.DEFAULTFRACTIONDIGITS);
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
   * utility method that calls formatters for date
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
   * @param text unixEpochAsString
   * @return formated string
   */
  public static String convDateTime(String text, DateFormat format) {
    Date date;
    try {
      date = new Date(Long.valueOf(text)*1000L);
    } catch (NumberFormatException e) {
      //legacy, the migration from date string to unix timestamp might have gone wrong
      //for some users
      try {
        date = TransactionDatabase.dateTimeFormat.parse(text);
      } catch (ParseException e1) {
        date = new Date();
      }
    }
    return format.format(date);
  }
  /**
   * utility method that calls formatters for amount
   * this method is called from adapters that give us the amount as String
   * @param text amount as String
   * @param currency
   * @return formated string
   */
  public static String convAmount(String text, Currency currency) {
    return convAmount(Long.valueOf(text), currency);
  }
  public static Currency getSaveInstance(String strCurrency) {
    try {
      return Currency.getInstance(strCurrency);
    } catch (IllegalArgumentException e) {
      Log.e("MyExpenses",strCurrency + " is not defined in ISO 4217");
      return Currency.getInstance(Locale.getDefault());
    }
  }
  /**
   * utility method that calls formatters for amount
   * this method can be called directly with Long values retrieved from db
   * @param text amount as String
   * @param currency
   * @return formated string
   */
  public static String convAmount(Long amount, Currency currency) {
    return formatCurrency(new Money(currency,amount));
  }
  /**
   * @return directory for storing backups and exports, null if external storage is not available
   */
  public static File requireAppDir() {
    if (!isExternalStorageAvailable())
      return null;
    File sd = Environment.getExternalStorageDirectory();
    File appDir = new File(sd, "myexpenses");
    appDir.mkdir();
    return appDir;
  }
  /**
   * Helper Method to Test if external Storage is Available
   * from http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html
   */
  public static boolean isExternalStorageAvailable() {
      boolean state = false;
      String extStorageState = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
          state = true;
      }
      return state;
  }
  
  public static boolean copy(File src, File dst) {
    FileInputStream srcStream = null;
    FileOutputStream dstStream = null;
    try {
      srcStream = new FileInputStream(src);
      dstStream = new FileOutputStream(dst);
      dstStream.getChannel().transferFrom(srcStream.getChannel(), 0, srcStream.getChannel().size());
      return true;
    } catch (FileNotFoundException e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
      return false;
    } catch (IOException e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
      return false;
    } finally {
      try { srcStream.close(); } catch (Exception e) {}
      try { dstStream.close(); } catch (Exception e) {}
    }
  }
  public static void share(Context ctx, ArrayList<File> files,String target, String mimeType) {
    URI uri = null;
    Intent intent;
    String scheme = "mailto";
    boolean multiple = files.size() > 1;
    if (!target.equals("")) {
      uri = Utils.validateUri(target);
      if (uri == null) {
        Toast.makeText(ctx,ctx.getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
        return;
      }
      scheme = uri.getScheme();
    }
    //if we get a String that does not include a scheme, we interpret it as a mail address
    if (scheme == null) {
      scheme = "mailto";
    }
    if (scheme.equals("ftp")) {
      if (multiple) {
        Toast.makeText(ctx,"sending multiple file through ftp is not supported", Toast.LENGTH_LONG).show();
        return;
      }
      intent = new Intent(android.content.Intent.ACTION_SENDTO);
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0)));
      intent.setDataAndType(android.net.Uri.parse(target),mimeType);
      if (!isIntentAvailable(ctx,intent)) {
        Toast.makeText(ctx,R.string.no_app_handling_ftp_available, Toast.LENGTH_LONG).show();
        return;
      }
      ctx.startActivity(intent);
    } else if (scheme.equals("mailto")) {
      if (multiple) {
        intent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for(File file : files) {
          uris.add(Uri.fromFile(file));
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
      } else {
        intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0)));
      }
      intent.setType(mimeType);
      if (uri != null) {
        String address = uri.getSchemeSpecificPart();
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ address });
      }
      intent.putExtra(Intent.EXTRA_SUBJECT,R.string.export_expenses);
      if (!isIntentAvailable(ctx,intent)) {
        Toast.makeText(ctx,R.string.no_app_handling_email_available, Toast.LENGTH_LONG).show();
        return;
      }
      //if we got mail address, we launch the default application
      //if we are called without target, we launch the chooser in order to make action more explicit
      if (uri != null) {
        ctx.startActivity(intent);
      } else {
        ctx.startActivity(Intent.createChooser(
            intent,ctx.getString(R.string.share_sending)));
      }
    } else {
      Toast.makeText(ctx,ctx.getString(R.string.share_scheme_not_supported,scheme), Toast.LENGTH_LONG).show();
      return;
    }
  }
  public static void setBackgroundFilter(View v, int c) {
    v.getBackground().setColorFilter(c,PorterDuff.Mode.MULTIPLY);
  }
  /**
   * Indicates whether the specified action can be used as an intent. This
   * method queries the package manager for installed packages that can
   * respond to an intent with the specified action. If no suitable package is
   * found, this method returns false.
   *
   * From http://android-developers.blogspot.fr/2009/01/can-i-use-this-intent.html
   *
   * @param context The application's environment.
   * @param action The Intent action to check for availability.
   *
   * @return True if an Intent with the specified action can be sent and
   *         responded to, false otherwise.
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
      final PackageManager packageManager = context.getPackageManager();
      List<ResolveInfo> list =
              packageManager.queryIntentActivities(intent,
                      PackageManager.MATCH_DEFAULT_ONLY);
      return list.size() > 0;
  }

  public static int getTextColorForBackground(int color) {
    int greyLevel = (int) (0.299 * Color.red(color)
        + 0.587 * Color.green(color)
        + 0.114 * Color.blue(color));
    return greyLevel > 127 ? Color.BLACK : Color.WHITE;
  }
  public static boolean verifyLicenceKey (String key) {
    String s = Secure.getString(MyApplication.getInstance().getContentResolver(),Secure.ANDROID_ID) + 
        MyApplication.CONTRIB_SECRET;
    Long l = (s.hashCode() & 0x00000000ffffffffL);
    return l.toString().equals(key);
  }
  public static void contribBuyDo(Activity ctx) {
//    Intent i = new Intent(Intent.ACTION_VIEW);
//    i.setData(Uri.parse(MyApplication.MARKET_PREFIX + "org.totschnig.myexpenses.contrib"));
//    if (Utils.isIntentAvailable(ctx,i)) {
//      ctx.startActivity(i);
//    } else {
      if (ctx instanceof FragmentActivity)
        DonateDialogFragment.newInstance().show(((FragmentActivity) ctx).getSupportFragmentManager(),"CONTRIB");
      else {
        //We are called from MyPreferenceActivity where support fragmentmanager is not available
        ctx.showDialog(R.id.DONATE_DIALOG);
      }
//    }
  }
  /**
   * @param ctx for retrieving resources
   * @param other if not null, all features except the one provided will be returned
   * @return construct a list of all contrib features to be included into a TextView
   */
  public static String getContribFeatureLabelsAsFormattedList(Context ctx,Feature other) {
    String result ="";
    Iterator<Feature> iterator = EnumSet.allOf(Feature.class).iterator();
    while (iterator.hasNext()) {
      Feature f = iterator.next();
      if (!f.equals(other)) {
        result += " - " + ctx.getString(ctx.getResources().getIdentifier("contrib_feature_" + f.toString() + "_label", "string", ctx.getPackageName()));
        if (iterator.hasNext())
          result += "<br>";
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
        for (int i=0; i<messageDigest.length; i++)
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
  public static <E extends Enum<E>> String joinEnum(Class<E> enumClass)  {
    String result ="";
    Iterator<E> iterator = EnumSet.allOf(enumClass).iterator();
    while (iterator.hasNext()) {
      result += "'" + iterator.next().name() + "'";
      if (iterator.hasNext())
        result += ",";
    }
    return result;
  }
  /**
   * Credit: https://groups.google.com/forum/?fromgroups#!topic/actionbarsherlock/Z8Ic8djq-3o
   * @param item
   * @param enabled
   */
//  public static void menuItemSetEnabled(Menu menu, int id, boolean enabled) {
//    MenuItem item = menu.findItem(id);
//    item.setEnabled(enabled);
//    item.getIcon().setAlpha(enabled ? 255 : 90);
//  }

  public static boolean doesPackageExist(Context context,String targetPackage) {
    try {
      context.getPackageManager().getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
     return false;
     }
     return true;
  }

  public static DateFormat localizedYearlessDateFormat() {
    Locale l = Locale.getDefault();
    String yearlessPattern = ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.SHORT,l))
        .toPattern().replaceAll("\\W?[Yy]+\\W?", "");
    return new SimpleDateFormat(yearlessPattern, l);
  }
}
