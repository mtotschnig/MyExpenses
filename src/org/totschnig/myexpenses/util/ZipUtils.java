package org.totschnig.myexpenses.util;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtils {

  
  /**
   * convenience method that allows to store the pictures into the backup
   * without copying them first
   * @param cacheDir
   * @param pictureDir
   * @throws Exception
   */
  public static void zipBackup(File cacheDir, File pictureDir, File destZipFile) throws Exception {
    ZipOutputStream zip = null;
    FileOutputStream fileWriter = null;
    /*
     * create the output stream to zip file result
     */
    fileWriter = new FileOutputStream(destZipFile);
    zip = new ZipOutputStream(fileWriter);
    /*
     * add the folder to the zip
     */
    addFolderToZip("", cacheDir, zip,true);
    addFolderToZip("", pictureDir, zip,false);
    /*
     * close the zip objects
     */
    zip.flush();
    zip.close();
  }
  
  /*
   * zip the folders
   */
  public static void zipFolder(File srcFolder, File destZipFile) throws Exception {
      ZipOutputStream zip = null;
      FileOutputStream fileWriter = null;
      /*
       * create the output stream to zip file result
       */
      fileWriter = new FileOutputStream(destZipFile);
      zip = new ZipOutputStream(fileWriter);
      /*
       * add the folder to the zip
       */
      addFolderToZip("", srcFolder, zip,true);
      /*
       * close the zip objects
       */
      zip.flush();
      zip.close();
  }

  /*
   * recursively add files to the zip files
   */
  private static void addFileToZip(String path, File srcFile,
      ZipOutputStream zip) throws Exception {

    /*
     * write the file to the output
     */
    byte[] buf = new byte[1024];
    int len;
    FileInputStream in = new FileInputStream(srcFile);
    String filePath = path + (path.equals("") ? "" : "/") + srcFile.getName();
    zip.putNextEntry(new ZipEntry(filePath));
    while ((len = in.read(buf)) > 0) {
      /*
       * Write the Result
       */
      zip.write(buf, 0, len);
    }
  }

  /*
   * add folder to the zip file
   */
  private static void addFolderToZip(String path, File srcFolder, ZipOutputStream zip, boolean strip) throws Exception {

      /*
       * check the empty folder
       */
      if (srcFolder.list().length == 0) {
          zip.putNextEntry(new ZipEntry(path + "/" + srcFolder.getName() + "/"));
      } else {
          /*
           * list the files in the folder
           */
          String dirPath = strip ? "" : (path + (path.equals("") ? "" : "/") + srcFolder.getName());
          for (File file : srcFolder.listFiles()) {
              if (file.isDirectory()) {
                  addFolderToZip(dirPath, file, zip,false);
              } else {
                  addFileToZip(dirPath,file, zip);
              }
          }
      }
  }
  /**
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
        File newFile = new File(dirOut,ze.getName());
        newFile.getParentFile().mkdirs();
        if(ze.isDirectory()) {
          newFile.mkdir();
        } else {
          FileOutputStream fout = new FileOutputStream(newFile);
          long startTime = System.currentTimeMillis();

//          for (int c = zin.read(); c != -1; c = zin.read()) {
//            fout.write(c);

          byte[] buffer = new byte[1024];
          int count;
          while ((count = zin.read(buffer)) != -1) {
              fout.write(buffer, 0, count);
          }
          long endTime = System.currentTimeMillis();

          Log.d("DEBUG","That took " + (endTime - startTime) + " milliseconds");
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