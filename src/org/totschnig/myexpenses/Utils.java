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

package org.totschnig.myexpenses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Util class with helper methods
 * @author Michael Totschnig
 *
 */
public class Utils {
  public static String getDefaultDecimalSeparator() {
    String sep = ".";
    int sdk =  Build.VERSION.SDK_INT;
    //there are different intricacies of bug http://code.google.com/p/android/issues/detail?id=2626
    //on Gingerbread, the numeric keyboard of the default input method
    //does not have a , thus we default to . as decimal separator
    if (sdk == 8 || sdk == 9) {
      return sep;
    }
   NumberFormat nfDLocal = NumberFormat.getNumberInstance();
    if (nfDLocal instanceof DecimalFormat) {
      DecimalFormatSymbols symbols = ((DecimalFormat)nfDLocal).getDecimalFormatSymbols();
      sep=String.valueOf(symbols.getDecimalSeparator());
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
  /**
   * formats an amount with a currency
   * @param amount
   * @param currency
   * @return formated string
   */
  static String formatCurrency(Money money) {
    BigDecimal amount = money.getAmountMajor();
    Currency currency = money.getCurrency();
    return formatCurrency(amount,currency);
  }
  static String formatCurrency(BigDecimal amount, Currency currency) {
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    int fractionDigits = currency.getDefaultFractionDigits();
    nf.setCurrency(currency);
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    return nf.format(amount);
  }
  /**
   * utility method that calls formatters for date
   * @param text
   * @return formated string
   */
  static String convDate(String text) {
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM HH:mm");
    return formatter.format(Timestamp.valueOf(text));
  }
  /**
   * utility method that calls formatters for amount
   * @param text amount as String retrieved from DB (stored as int minor unit)
   * @param currency 
   * @return formated string
   */
  static String convAmount(String text, Currency currency) {
    return formatCurrency(new Money(currency,Long.valueOf(text)));
  }
  //TODO: create generic function
  static String[] getStringArrayFromCursor(Cursor c, String field) {
    String[] result = new String[c.getCount()];
    if(c.moveToFirst()){
     for (int i = 0; i < c.getCount(); i++){
       result[i] = c.getString(c.getColumnIndex(field));
       c.moveToNext();
     }
    }
    return result;
  }
  static long[] getLongArrayFromCursor(Cursor c, String field) {
    long[] result = new long[c.getCount()];
    if(c.moveToFirst()){
     for (int i = 0; i < c.getCount(); i++){
       result[i] = c.getInt(c.getColumnIndex(field));
       c.moveToNext();
     }
    }
    return result;
  }
  
  /**
   * @return directory for storing backups and exports, null if external storage is not available
   */
  static File requireAppDir() {
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
  static boolean isExternalStorageAvailable() {
      boolean state = false;
      String extStorageState = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
          state = true;
      }
      return state;
  }
  
  static boolean copy(File src, File dst) {
    FileChannel srcC;
    try {
      srcC = new FileInputStream(src).getChannel();
      FileChannel dstC = new FileOutputStream(dst).getChannel();
      dstC.transferFrom(srcC, 0, srcC.size());
      srcC.close();
      dstC.close();
      return true;
    } catch (FileNotFoundException e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
    } catch (IOException e) {
      Log.e("MyExpenses",e.getLocalizedMessage());
    }
    return false;
  }
  
  static void share(Context context,File file,String target) {
    URI uri = null;
    Intent intent;
    String scheme = "mailto";
    if (!target.equals("")) {
      try {
        uri = new URI(target);
      } catch (URISyntaxException e1) {
        Toast.makeText(context,context.getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
        return;
      }
      scheme = uri.getScheme();
    }
    //if we get a String that does not include a scheme, we interpret it as a mail address
    if (scheme == null) {
    	scheme = "mailto";
    }
    final PackageManager packageManager = context.getPackageManager();
    if (scheme.equals("ftp")) {
  		intent = new Intent(android.content.Intent.ACTION_SENDTO);
  		intent.setData(android.net.Uri.parse(target));
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
  		//intent.putExtra("ftp_username", "michael");
  		//intent.putExtra("ftp_password", "12august12");
      //intent = new Intent(context, FtpTransfer.class);
      //intent.putExtra("target",uri);
      //intent.putExtra("source",file.getAbsolutePath());
      if (packageManager.queryIntentActivities(intent,0).size() == 0) {
        Toast.makeText(context,"no_app_handling_ftp_available", Toast.LENGTH_LONG).show();
        return;
      }
      context.startActivity(intent);
    } else if (scheme.equals("mailto")) {
      intent = new Intent(android.content.Intent.ACTION_SEND);
      intent.setType("text/qif");
      if (uri != null) {
        String address = uri.getSchemeSpecificPart();
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ address });
      }
      intent.putExtra(Intent.EXTRA_SUBJECT,R.string.export_expenses);
      intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
      if (packageManager.queryIntentActivities(intent,0).size() == 0) {
        Toast.makeText(context,R.string.no_app_handling_email_available, Toast.LENGTH_LONG).show();
        return;
      }
      //if we got mail address, we launch the default application
      //if we are called without target, we launch the chooser in order to make action more explicit
      if (uri != null) {
        context.startActivity(intent);
      } else {
        context.startActivity(Intent.createChooser(
            intent,context.getString(R.string.share_sending)));
      }
    } else {
      Toast.makeText(context,context.getString(R.string.share_scheme_not_supported,target), Toast.LENGTH_LONG).show();
      return;
    }
  }
  
  
  /**
   * simple two level category tree, used for storing categories extracted from Grisbi XML
   * guarantees that children are always added through root
   *
   */
  public static class CategoryTree {
    private HashMap<Integer,CategoryTree> children;
    private String label;
    private int total;
    private boolean rootP;
 
    public CategoryTree(String label) {
      this(label,true);
    }
    public CategoryTree(String label, boolean rootP) {
      children = new HashMap<Integer,CategoryTree>();
      this.setLabel(label);
      total = 0;
      this.rootP = rootP;
    }
    
    /**
     * @param label
     * @param id
     * @param parent
     * adds a new CategoryTree under parent with a given label and id
     * This operation is only allowed for the root tree, it is not allowed to add directly to
     * subtrees (throws {@link UnsupportedOperationException}). If parent is 0, a top level 
     * category tree is created. If there is no parent with id parent, the method returns without
     * creating a CategoryTree
     */
    public boolean add(String label, Integer id, Integer parent) {
      if (!rootP) {
        throw new UnsupportedOperationException();
      }
      if (parent == 0) {
        addChild(label,id);
      } else {
        CategoryTree parentCat = children.get(parent);
        if (parentCat == null) {
          return false;
        }
        parentCat.addChild(label, id);
      }
      total++;
      return true;
    }
    private void addChild(String label, Integer id) {
      children.put(id,new CategoryTree(label,false));
    }
    
    public HashMap<Integer,CategoryTree> children() {
      return children;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }
    public int getTotal() {
      return total;
    }
  }  
  /**
   * represents a tuple of success flag, and message as an R id
   * @author Michael Totschnig
   *
   */
  public static class Result {
    /**
     * true represents success, false failure
     */
    public boolean success;
    /**
     * a string id from {@link R} for i18n and joining with an argument
     */
    public int message;
    
    /**
     * optional argument to be passed to getString when resolving message id
     */
    public Object[] extra;
    
    public Result(boolean success) {
      this.success = success;
    }
   
    public Result(boolean success,int message) {
      this.success = success;
      this.message = message;
    }

    public Result(boolean success,int message,Object... extra) {
      this.success = success;
      this.message = message;
      this.extra = extra;
    }
  }
}
