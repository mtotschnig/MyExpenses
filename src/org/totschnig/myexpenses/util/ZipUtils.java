//Credits: http://www.jondev.net/articles/Zipping_Files_with_Android_%28Programmatically%29
package org.totschnig.myexpenses.util;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtils {
  private static final int BUFFER = 2048;

  public static boolean zip(File[] _files, File _zipFile) {
    try  {
      BufferedInputStream origin = null;
      FileOutputStream dest = new FileOutputStream(_zipFile);

      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

      byte data[] = new byte[BUFFER];

      for(int i=0; i < _files.length; i++) {
        Log.d("Compress", "Adding: " + _files[i]);
        FileInputStream fi = new FileInputStream(_files[i]);
        origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(_files[i].getName());
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
          out.write(data, 0, count);
        }
        origin.close();
      }
      out.close();
      return true;
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  /**
   * This is not tested on hierarchical zip files, currently only used on flat archives
   * @param fileIn
   * @param dirOut
   * @return true on success
   */
  public static boolean unzip(InputStream fileIn, File dirOut) {
    try  {
      ZipInputStream zin = new ZipInputStream(fileIn);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        Log.v("Decompress", "Unzipping " + ze.getName());
        if(ze.isDirectory()) {
          throw new UnsupportedOperationException("currently only flat archives are supported");
        } else {
          FileOutputStream fout = new FileOutputStream(new File(dirOut,ze.getName()));
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }
          zin.closeEntry();
          fout.close();
        }
      }
      zin.close();
      return true;
    } catch(Exception e) {
      Log.e("Decompress", "unzip", e);
      return false;
    }
  }
}