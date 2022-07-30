package org.totschnig.myexpenses.util;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.BackupUtilsKt;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.crypt.EncryptionHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;


public class ZipUtils {


  public static final String PICTURES = Environment.DIRECTORY_PICTURES;

  private ZipUtils() {
  }

  public static void zipBackup(File cacheDir, DocumentFile destZipFile, String password)
      throws IOException, GeneralSecurityException {
    final OutputStream out = MyApplication.getInstance().getContentResolver().openOutputStream(destZipFile.getUri());
    ZipOutputStream zip = new ZipOutputStream(TextUtils.isEmpty(password) ? out : EncryptionHelper.encrypt(out, password));
    /*
     * add the folder to the zip
     */
    addFileToZip("", BackupUtilsKt.getBackupDbFile(cacheDir), zip);
    addFileToZip("", BackupUtilsKt.getBackupPrefFile(cacheDir), zip);
    Cursor c = MyApplication.getInstance().getContentResolver()
        .query(TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter(
            TransactionProvider.QUERY_PARAMETER_DISTINCT, "1").build(),
            new String[]{DatabaseConstants.KEY_PICTURE_URI},
            DatabaseConstants.KEY_PICTURE_URI + " IS NOT NULL",
            null, null);
    if (c != null) {
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
                //File has been removed
                //Timber.e(e);
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
    addFolderToZip("", srcFolder, zip, true);
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
    addInputStreamToZip(filePath, in, zip);
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
          addFolderToZip(dirPath, file, zip, false);
        } else {
          addFileToZip(dirPath, file, zip);
        }
      }
    }
  }

  public static void unzip(InputStream fileIn, File dirOut, @Nullable String password)
      throws IOException, GeneralSecurityException {
    ZipInputStream zin = new ZipInputStream(TextUtils.isEmpty(password) ? fileIn : EncryptionHelper.decrypt(fileIn, password));
    ZipEntry ze;
    while ((ze = zin.getNextEntry()) != null) {
      Timber.v("Unzipping %s", ze.getName());
      File newFile = new File(dirOut, ze.getName());
      String canonicalPath = newFile.getCanonicalPath();
      if (!canonicalPath.startsWith(dirOut.getCanonicalPath())) {
        throw new SecurityException("Path Traversal Vulnerability");
      }
      newFile.getParentFile().mkdirs();
      if (ze.isDirectory()) {
        newFile.mkdir();
      } else {
        FileOutputStream fout = new FileOutputStream(newFile);
        long startTime = System.currentTimeMillis();

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
  }
}