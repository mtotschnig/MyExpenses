package org.totschnig.myexpenses.util.io;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCopyUtils {

  public static boolean copy(File src, File dst) {
    try (FileInputStream srcStream = new FileInputStream(src);
         FileOutputStream dstStream = new FileOutputStream(dst)) {
      dstStream.getChannel().transferFrom(srcStream.getChannel(), 0,
          srcStream.getChannel().size());
      return true;
    } catch (IOException e) {
      CrashHandler.report(e);
      return false;
    }
  }

  /**
   * copy src uri to dest uri
   */
  public static void copy(ContentResolver contentResolver, @NonNull Uri src, @NonNull Uri dest) throws IOException {
    try (InputStream input = contentResolver.openInputStream(src);
         OutputStream output = contentResolver.openOutputStream(dest)) {
      if (input == null) {
        throw new IOException("Could not open InputStream " + src);
      }
      if (output == null) {
        throw new IOException("Could not open OutputStream " + dest);
      }
      copy(input, output);
    }
  }

  public static void copy(@NonNull InputStream input, @NonNull OutputStream output) throws IOException {
    final byte[] buffer = new byte[1024];
    int read;

    while ((read = input.read(buffer)) != -1) {
      output.write(buffer, 0, read);
    }
    output.flush();
  }
}
