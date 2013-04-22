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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
    String msg = ctx.getClass() == MyExpenses.class ? (ctx.getString(R.string.version_32_upgrade_info) + " ") : "";
    return new AlertDialog.Builder(ctx)
    .setMessage(msg + ctx.getString(R.string.no_app_handling_ftp_available))
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
           ctx.dismissDialog(R.id.FTP_DIALOG_ID);
           if (ctx.getClass() == MyExpenses.class)
             ctx.showDialog(R.id.VERSION_DIALOG_ID);
           Intent intent = new Intent(Intent.ACTION_VIEW);
           intent.setData(Uri.parse("market://details?id=org.totschnig.sendwithftp"));
           if (isIntentAvailable(ctx,intent)) {
             ctx.startActivity(intent);
           } else {
             Toast.makeText(ctx.getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
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
  public static Integer usagesLeft(String feature) {
    return 5 - MyApplication.db().getContribFeatureUsages(feature);
  }
  public static void recordUsage(String feature) {
    MyApplication.db().incrFeatureUsages(feature);
  }
  public static Dialog contribDialog(final Activity ctx,final String feature) {
    final Integer usagesLeft = usagesLeft(feature);
    CharSequence message = Html.fromHtml(String.format(ctx.getString(
      R.string.dialog_contrib_reminder,
      ctx.getString(ctx.getResources().getIdentifier("contrib_feature_" + feature + "_label", "string", ctx.getPackageName())),
      usagesLeft > 0 ? ctx.getString(R.string.dialog_contrib_usage_count,usagesLeft) : ctx.getString(R.string.dialog_contrib_no_usages_left))));
    return createMessageDialogWithCustomButtons(
      new ContextThemeWrapper(ctx, MyApplication.getThemeId()) {
        public void onDialogButtonClicked(View v) {
          if (v.getId() == R.id.CONTRIB_PLAY_COMMAND_ID) {
            ctx.dismissDialog(R.id.CONTRIB_DIALOG_ID);
            viewContribApp(ctx);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=org.totschnig.myexpenses.contrib"));
            if (isIntentAvailable(ctx,intent)) {
              ctx.startActivity(intent);
            } else {
              Toast.makeText(ctx.getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
            }
            ((ContribIFace)ctx).contribFeatureNotCalled();
          } else {
            if (usagesLeft > 0) {
              //we remove the dialog, in order to have it display updated usage count on next display
              ctx.removeDialog(R.id.CONTRIB_DIALOG_ID);
              ((ContribIFace)ctx).contribFeatureCalled(feature);
            } else {
              ctx.dismissDialog(R.id.CONTRIB_DIALOG_ID);
              ((ContribIFace)ctx).contribFeatureNotCalled();
            }
          }
        }
      },
      message,R.id.CONTRIB_PLAY_COMMAND_ID,null, R.string.dialog_contrib_yes,R.string.dialog_contrib_no)
    .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            ((ContribIFace)ctx).contribFeatureNotCalled();
          }
        })
    .create();
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
  
  public static void setBackgroundFilter(View v, int c) {
    v.getBackground().setColorFilter(c,PorterDuff.Mode.MULTIPLY);
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

  /**
   * @return an AlertDialog.Builder with R.layout.messagedialog as layout
   */
  public static AlertDialog.Builder createMessageDialog(Context ctx, int message,int command,Object tag) {
    return createMessageDialogWithCustomButtons(ctx,message,command,tag,android.R.string.yes,android.R.string.no);
  }
  /**
   * @return an AlertDialog.Builder with R.layout.messagedialog as layout
   */
  public static AlertDialog.Builder createMessageDialogWithCustomButtons(
      Context ctx, CharSequence message,int command,Object tag, int yesButton, int noButton) {
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.messagedialog, null);
    TextView tv = (TextView)view.findViewById(R.id.message_text);
    tv.setText(message);
    setDialogTwoButtons(view,
        yesButton,command,tag,
        noButton,0,null
    );
    return new AlertDialog.Builder(ctx)
      .setView(view);
  }
  public static AlertDialog.Builder createMessageDialogWithCustomButtons(
      Context ctx, int  message,int command,Object tag, int yesButton, int noButton) {
    return createMessageDialogWithCustomButtons(ctx,ctx.getString(message),command,tag,yesButton,noButton);
  }
  /**
   * one button centered takes up 33% width
   */
  static void setDialogOneButton(View view,
      int neutralString, int neutralCommandId,Object neutralTag) {
    setButton((Button) view.findViewById(R.id.NEUTRAL_BUTTON),neutralString,neutralCommandId,neutralTag);
    view.findViewById(R.id.POSITIVE_BUTTON).setVisibility(View.INVISIBLE);
    view.findViewById(R.id.NEGATIVE_BUTTON).setVisibility(View.INVISIBLE);
  }
  /**
   * two buttons 50% width each
   */
  static void setDialogTwoButtons(View view,
      int positiveString, int positiveCommandId,Object positiveTag,
      int negativeString, int negativeCommandId,Object negativeTag) {
    setButton((Button) view.findViewById(R.id.POSITIVE_BUTTON),positiveString,positiveCommandId,positiveTag);
    setButton((Button) view.findViewById(R.id.NEGATIVE_BUTTON),negativeString,negativeCommandId,negativeTag);
    view.findViewById(R.id.NEUTRAL_BUTTON).setVisibility(View.GONE);
  }
  /**
   * three buttons 33% width each
   */
  static void setDialogThreeButtons(View view,
      int positiveString, int positiveCommandId,Object positiveTag,
      int neutralString, int neutralCommandId,Object neutralTag,
      int negativeString, int negativeCommandId,Object negativeTag) {
    setButton((Button) view.findViewById(R.id.POSITIVE_BUTTON),positiveString,positiveCommandId,positiveTag);
    setButton((Button) view.findViewById(R.id.NEUTRAL_BUTTON),neutralString,neutralCommandId,neutralTag);
    setButton((Button) view.findViewById(R.id.NEGATIVE_BUTTON),negativeString,negativeCommandId,negativeTag);
  }
  /**
   * set String s and Command c on Button b
   * if s i null, hide button
   * @param b
   * @param s
   * @param c
   */
  private static void setButton(Button b, int s, int c,Object tag) {
    b.setText(s);
    if (c != 0) {
      b.setId(c);
      if (tag != null)
        b.setTag(tag);
    }
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
  public static boolean doesPackageExist(Context context,String targetPackage){
    PackageManager pm=context.getPackageManager();
    try {
     PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
     return false;
     }  
     return true;
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
  public static void viewContribApp(Activity ctx) {
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setData(Uri.parse("market://details?id=org.totschnig.myexpenses.contrib"));
    if (Utils.isIntentAvailable(ctx,i)) {
      ctx.startActivity(i);
    } else {
      Toast.makeText(ctx.getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
    }
  }
}
