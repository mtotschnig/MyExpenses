package org.totschnig.myexpenses.util;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtils {


  public static final String PICTURES = "pictures";

  /**
   * convenience method that allows to store the pictures into the backup
   * without copying them first
   * @param cacheDir
   * @throws Exception
   */
  public static void zipBackup(File cacheDir, File destZipFile) throws IOException {
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
    addFileToZip("", MyApplication.getBackupDbFile(cacheDir), zip);
    addFileToZip("", MyApplication.getBackupPrefFile(cacheDir), zip);
    Cursor c= MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.TRANSACTIONS_URI,
            new String[]{DatabaseConstants.KEY_PICTURE_URI},
            DatabaseConstants.KEY_PICTURE_URI + " IS NOT NULL",
            null,null);
    if (c!=null) {
      if (c.moveToFirst()) {
        do {
          Uri imageFileUri = Uri.parse(c.getString(0));
          if (imageFileUri.getScheme().equals("file")) {
            File imageFile = new File(imageFileUri.getPath());
            addFileToZip(PICTURES,imageFile,zip);
          } else {
            InputStream in = MyApplication.getInstance().getContentResolver().openInputStream(imageFileUri);
            addInputStreamToZip(PICTURES+"/"+imageFileUri.getLastPathSegment(),
                in,
                zip);
            in.close();
          }
        } while (c.moveToNext());
      }
      c.close();
    }
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
      ZipOutputStream zip) throws IOException {

    FileInputStream in = new FileInputStream(srcFile);
    String filePath = path + (path.equals("") ? "" : "/") + srcFile.getName();
    addInputStreamToZip(filePath,in,zip);
    in.close();
  }

  private static void addInputStreamToZip(String path, InputStream in,
                                   ZipOutputStream zip) throws IOException {

    /*
     * write the file to the output
     */
    byte[] buf = new byte[1024];
    int len;
    zip.putNextEntry(new ZipEntry(path));
    while ((len = in.read(buf)) > 0) {
      /*
       * Write the Result
       */
      zip.write(buf, 0, len);
    }
    in.close();
  }

  /*
   * add folder to the zip file
   */
  private static void addFolderToZip(String path, File srcFolder, ZipOutputStream zip, boolean strip) throws IOException {

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