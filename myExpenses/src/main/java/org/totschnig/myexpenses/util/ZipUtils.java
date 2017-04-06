package org.totschnig.myexpenses.util;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import timber.log.Timber;


public class ZipUtils {


  public static final String PICTURES = Environment.DIRECTORY_PICTURES;

  private ZipUtils() {
  }

  /**
   * convenience method that allows to store the pictures into the backup
   * without copying them first
   * @param cacheDir
   * @param destZipFile
   * @throws Exception
   */
  public static void zipBackup(File cacheDir, DocumentFile destZipFile) throws IOException {
    /*
     * create the output stream to zip file result
     */
    ZipOutputStream zip = new ZipOutputStream(MyApplication.getInstance().getContentResolver().openOutputStream(destZipFile.getUri()));
    /*
     * add the folder to the zip
     */
    addFileToZip("", BackupUtils.getBackupDbFile(cacheDir), zip);
    addFileToZip("", BackupUtils.getBackupPrefFile(cacheDir), zip);
    Cursor c= MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter(
                TransactionProvider.QUERY_PARAMETER_DISTINCT,"1").build(),
            new String[]{DatabaseConstants.KEY_PICTURE_URI},
            DatabaseConstants.KEY_PICTURE_URI + " IS NOT NULL",
            null,null);
    if (c!=null) {
      if (c.moveToFirst()) {
        try {
          do {
            Uri imageFileUri = Uri.parse(c.getString(0));
            if (imageFileUri.getScheme().equals("file")) {
              File imageFile = new File(imageFileUri.getPath());
              if (imageFile.exists()) {
                addFileToZip(PICTURES, imageFile, zip);
              }
            } else {
              InputStream in;
              try {
                in = MyApplication.getInstance().getContentResolver().openInputStream(imageFileUri);
                addInputStreamToZip(PICTURES + "/" + imageFileUri.getLastPathSegment(),
                    in,
                    zip);
                in.close();
              } catch (FileNotFoundException e) {
                e.printStackTrace();
              }
            }
          } while (c.moveToNext());
        } catch (IOException e) {
          c.close();
          throw e;
        }
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
      ZipOutputStream zip;
      FileOutputStream fileWriter;
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
      ZipEntry ze;
      while ((ze = zin.getNextEntry()) != null) {
        Timber.v("Unzipping " + ze.getName());
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

          Timber.d("That took %d milliseconds", (endTime - startTime));
          zin.closeEntry();
          fout.close();
        }
      }
      zin.close();
      return true;
    } catch(Exception e) {
      Timber.e(e);
      return false;
    }
  }
}