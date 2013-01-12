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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
  /**
   * @return Dialog to be used from Preference,
   * and from version update
   */
  public static Dialog sendWithFTPDialog(final Activity ctx) {
    String msg = ctx.getClass() == MyExpenses.class ? ctx.getString(R.string.version_32_upgrade_info) : "";
    return new AlertDialog.Builder(ctx)
    .setMessage(msg + " " + ctx.getString(R.string.no_app_handling_ftp_available))
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
           ctx.dismissDialog(R.id.FTP_DIALOG_ID);
           if (ctx.getClass() == MyExpenses.class)
             ctx.showDialog(R.id.VERSION_DIALOG_ID);
           Intent intent = new Intent(Intent.ACTION_VIEW);
           intent.setData(Uri.parse("market://details?id=org.totschnig.sendwithftp"));
           if (ctx.getPackageManager().queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
             ctx.startActivity(intent);
           } else {
             Toast.makeText(ctx.getBaseContext(),"Unable to open Google Play", Toast.LENGTH_LONG).show();
           }
         }
      })
    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        ctx.dismissDialog(R.id.FTP_DIALOG_ID);
        if (ctx.getClass() == MyExpenses.class)
          ctx.showDialog(R.id.VERSION_DIALOG_ID);
      }
    }).create();
  }
  
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
  static String convDate(String text, SimpleDateFormat format) {
    return format.format(Timestamp.valueOf(text));
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
  static Long[] getLongArrayFromCursor(Cursor c, String field) {
    Long[] result = new Long[c.getCount()];
    if(c.moveToFirst()){
     for (int i = 0; i < c.getCount(); i++){
       result[i] = c.getLong(c.getColumnIndex(field));
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
