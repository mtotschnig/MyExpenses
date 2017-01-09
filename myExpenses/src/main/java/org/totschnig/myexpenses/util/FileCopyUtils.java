package org.totschnig.myexpenses.util;

import android.net.Uri;
import android.util.Log;

import org.totschnig.myexpenses.MyApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCopyUtils {

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
      copy(input, output);
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

  public static void copy(InputStream input, OutputStream output) throws IOException {
    final byte[] buffer = new byte[1024];
    int read;

    while ((read = input.read(buffer)) != -1) {
      output.write(buffer, 0, read);
    }
    output.flush();
  }
}
