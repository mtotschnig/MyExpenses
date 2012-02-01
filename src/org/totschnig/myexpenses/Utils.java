package org.totschnig.myexpenses;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class Utils {
  //should maybe go to a utility class
  //see http://www.ibm.com/developerworks/java/library/j-numberformat/
  static Float validateNumber(String strFloat) {
    ParsePosition pp;
    NumberFormat nfDLocal = NumberFormat.getNumberInstance();
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
  
  static int ftpUpload(File file, String ftpTarget) {
    FTPClient mFTP = new FTPClient();
    URI uri = null;
    try {
      uri = new URI(ftpTarget);
    } catch (URISyntaxException e1) {
      // TODO Auto-generated catch block
      return R.string.ftp_uri_malformed;
    }
    String host = uri.getHost();
    if (host == null)
      return R.string.ftp_uri_malformed;
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
        return result ? R.string.ftp_success : R.string.ftp_failure;
    } catch (SocketException e) {
        // TODO Auto-generated catch block
        return R.string.ftp_socket_exception;
    } catch (IOException e) {
        // TODO Auto-generated catch block
        return R.string.ftp_io_exception;
    }
  }
}
