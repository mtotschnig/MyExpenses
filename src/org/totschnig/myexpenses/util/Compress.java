//Credits: http://www.jondev.net/articles/Zipping_Files_with_Android_%28Programmatically%29
package org.totschnig.myexpenses.util;

import android.util.Log; 
import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.File;
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.util.zip.ZipEntry; 
import java.util.zip.ZipOutputStream; 
 
 
public class Compress { 
  private static final int BUFFER = 2048; 
 
  private File[] _files; 
  private File _zipFile; 
 
  public Compress(File[] files, File zipFile) { 
    _files = files; 
    _zipFile = zipFile; 
  } 
 
  public void zip() { 
    try  { 
      BufferedInputStream origin = null; 
      FileOutputStream dest = new FileOutputStream(_zipFile); 
 
      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest)); 
 
      byte data[] = new byte[BUFFER]; 
 
      for(int i=0; i < _files.length; i++) { 
        Log.v("Compress", "Adding: " + _files[i]); 
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
    } catch(Exception e) { 
      e.printStackTrace(); 
    }
  }

} 