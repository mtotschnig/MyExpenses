/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Currency;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

/**
 * Util class with helper methods
 * @author Michael Totschnig
 *
 */
public class Utils {
  //see http://www.ibm.com/developerworks/java/library/j-numberformat/
  /**
   * <a href="http://www.ibm.com/developerworks/java/library/j-numberformat/">http://www.ibm.com/developerworks/java/library/j-numberformat/</a>
   * @param strFloat parsed as float with the number format defined in the locale
   * @return the float retrieved from the string or null if parse did not succeed
   */
  public static Float validateNumber(String strFloat) {
    ParsePosition pp;
    NumberFormat nfDLocal = NumberFormat.getNumberInstance();
    nfDLocal.setGroupingUsed(false);
    pp = new ParsePosition( 0 );
    pp.setIndex( 0 );
    Number n = nfDLocal.parse(strFloat,pp);
    if( strFloat.length() != pp.getIndex() || 
        n == null )
    {
      return null;
    } else {
      return n.floatValue();
    }
  }
  /**
   * formats an amount with a currency
   * @param amount
   * @param currency
   * @return formated string
   */
  static String formatCurrency(float amount, Currency currency) {
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    nf.setCurrency(currency);
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
   * @param text amount as String retrieved from DB or UI
   * @param currency 
   * @return formated string
   */
  static String convAmount(String text, Currency currency) {
    float amount;
    try {
      amount = Float.valueOf(text);
    } catch (NumberFormatException e) {
      amount = 0;
    }
    return formatCurrency(amount,currency);
  }
  
  static File requireAppDir() {
    File sd = Environment.getExternalStorageDirectory();
    File appDir = new File(sd, "myexpenses");
    appDir.mkdir();
    return appDir;
  }
  
  static void share(Context context,File file,String target) {
    URI uri = null;
    try {
      uri = new URI(target);
    } catch (URISyntaxException e1) {
      Toast.makeText(context,context.getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
      return;
    }
    String scheme = uri.getScheme();
    if (scheme.equals("ftp")) {
      new Utils.FtpAsyncTask(context,file,uri).execute();
      return;
    } else if (scheme.equals("mailto")) {
      final PackageManager packageManager = context.getPackageManager();
      final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
      emailIntent.setType("text/qif");
      emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ uri.getSchemeSpecificPart()});
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Expenses export");
      emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
      if (packageManager.queryIntentActivities(emailIntent,0).size() == 0) {
        Toast.makeText(context,"No app handling email available", Toast.LENGTH_LONG).show();
        return;
      }
      
      context.startActivity(emailIntent);
    }
    else {
      Toast.makeText(context,context.getString(R.string.share_scheme_not_supported,target), Toast.LENGTH_LONG).show();
    }
  }
  
  /**
   * represents a tuple of success flag, and message as an R id
   * @author Michael Totschnig
   *
   */
  static class Result {
    /**
     * true represents success, false failure
     */
    public boolean success;
    /**
     * a string id from {@link R} for i18n and joining with an argument
     */
    public int message;
   
    public Result(boolean success,int message) {
      this.success = success;
      this.message = message;
    }
  }
  static class FtpAsyncTask extends AsyncTask<Void, Void, Result> {
    private Context context;
    private URI target;
    private File file;
    ProgressDialog mProgressDialog;
    
    public FtpAsyncTask(Context context,File file,URI uri) {
      this.context = context;
      this.target = uri;
      this.file = file;
    }
    protected void onPreExecute() {
       mProgressDialog = ProgressDialog.show(context, "", 
          "Uploading. Please wait...", true);
    }
    @Override
    protected Result doInBackground(Void... params) {
      boolean result;
      //malformed:
      //String ftpTarget = "bad.uri";
      //bad password:
      //String ftpTarget = "ftp://michael:foo@10.0.0.2/";
      //bad directory:
      //String ftpTarget = "ftp://michael:foo@10.0.0.2/foobar/";
      FTPClient mFTP = new FTPClient();
      String host = target.getHost();
      if (host == null)
        return new Result(false,R.string.ftp_uri_malformed);
      String username = target.getUserInfo();
      String password = "";
      String path = target.getPath();
      if (username != null)
        {
        int ci = username.indexOf(':');
          if (ci != -1) {
            password = username.substring(ci + 1);
            username = username.substring(0, ci);
          }
        }
      else {
        username = "anonymous";
      }
      try {
          // Connect to FTP Server
          mFTP.connect(host);
          
          if (!mFTP.login(username,password)) {
            return new Result(false, R.string.ftp_login_failure);
          }
          
          if (!mFTP.setFileType(FTP.ASCII_FILE_TYPE)) {
            return new Result(false, R.string.ftp_setFileType_failure);
          }
          mFTP.enterLocalPassiveMode();
          if (!mFTP.changeWorkingDirectory(path)) {
            return new Result(false, R.string.ftp_changeWorkingDirectory_failure);
          }
          
          // Prepare file to be uploaded to FTP Server
          FileInputStream ifile = new FileInputStream(file);
          
          // Upload file to FTP Server
          result = mFTP.storeFile(file.getName(),ifile);
          mFTP.disconnect();
          return new Result(result, result ? R.string.ftp_success : R.string.ftp_failure);
      } catch (SocketException e) {
          return new Result(false, R.string.ftp_socket_exception);
      } catch (IOException e) {
          return new Result(false,R.string.ftp_io_exception);
      }
    }
    protected void onPostExecute(Result result) {
      mProgressDialog.dismiss();
      super.onPostExecute(result);
      String ftp_result = context.getString(result.message,target.toString());
      Toast.makeText(context,ftp_result, Toast.LENGTH_LONG).show();
    }
  }
}
