package org.totschnig.myexpenses.util.io;

import android.net.Uri;

import org.totschnig.myexpenses.MyApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class FileCopyUtils {

  public static boolean copy(File src, File dst) {
    try (FileInputStream srcStream = new FileInputStream(src);
         FileOutputStream dstStream = new FileOutputStream(dst)) {
      dstStream.getChannel().transferFrom(srcStream.getChannel(), 0,
          srcStream.getChannel().size());
      return true;
    } catch (IOException e) {
      Timber.e(e);
      return false;
    }
  }

  /**
   * copy src uri to dest uri
   */
  public static void copy(Uri src, Uri dest) throws IOException {
    try (InputStream input = MyApplication.getInstance().getContentResolver()
        .openInputStream(src); OutputStream output = MyApplication.getInstance().getContentResolver()
        .openOutputStream(dest)) {
      if (input == null) {
        throw new IOException("Could not open InputStream " + src.toString());
      }
      if (output == null) {
        throw new IOException("Could not open OutputStream " + dest.toString());
      }
      copy(input, output);
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
