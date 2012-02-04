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
  static Float validateNumber(String strFloat) {
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
  
  /**
   * @param file File to be uploaded
   * @param ftpTarget FTP URL where the file should go to
   * @return a {@link Utils.Result} instance with success flag and diagnostic message resource id
   */
  static Result ftpUpload(File file, String ftpTarget) {
    FTPClient mFTP = new FTPClient();
    URI uri = null;
    try {
      uri = new URI(ftpTarget);
    } catch (URISyntaxException e1) {
      // TODO Auto-generated catch block
      return new Result(false,R.string.ftp_uri_malformed);
    }
    String host = uri.getHost();
    if (host == null)
      return new Result(false,R.string.ftp_uri_malformed);
    String username = uri.getUserInfo();
    String password = "";
    String path = uri.getPath();
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
        
        mFTP.login(username,password);
        mFTP.setFileType(FTP.ASCII_FILE_TYPE);
        mFTP.enterLocalPassiveMode();
        mFTP.changeWorkingDirectory(path);
        
        // Prepare file to be uploaded to FTP Server
        FileInputStream ifile = new FileInputStream(file);
        
        // Upload file to FTP Server
        boolean result = mFTP.storeFile(file.getName(),ifile);
        mFTP.disconnect();
        return new Result(result, result ? R.string.ftp_success : R.string.ftp_failure);
    } catch (SocketException e) {
        // TODO Auto-generated catch block
        return new Result(false, R.string.ftp_socket_exception);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        return new Result(false,R.string.ftp_io_exception);
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
}
