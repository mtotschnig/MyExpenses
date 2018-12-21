package org.totschnig.myexpenses.util;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
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
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import timber.log.Timber;

import static org.totschnig.myexpenses.util.crypt.EncryptionHelper.ENCRYPTION_IV_LENGTH;


public class ZipUtils {


  public static final String PICTURES = Environment.DIRECTORY_PICTURES;

  private ZipUtils() {
  }

  /**
   * convenience method that allows to store the pictures into the backup
   * without copying them first
   *
   * @param cacheDir
   * @param destZipFile
   * @throws IOException
   * @throws GeneralSecurityException
   */
  static void zipBackup(File cacheDir, DocumentFile destZipFile) throws IOException, GeneralSecurityException {
    /*
     * create the output stream to zip file result
     */
    SecretKey key = EncryptionHelper.generateSymmetricKeyFromPassword("PASSWORD");
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    final byte[] iv = new byte[ENCRYPTION_IV_LENGTH];
    new SecureRandom().nextBytes(iv);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    final OutputStream out = MyApplication.getInstance().getContentResolver().openOutputStream(destZipFile.getUri());
    out.write(cipher.getIV());
    ZipOutputStream zip = new ZipOutputStream(new CipherOutputStream(out, cipher));
    /*
     * add the folder to the zip
     */
    addFileToZip("", BackupUtils.getBackupDbFile(cacheDir), zip);
    addFileToZip("", BackupUtils.getBackupPrefFile(cacheDir), zip);
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

  /**
   * @param fileIn
   * @param dirOut
   * @return true on success
   */
  public static boolean unzip(InputStream fileIn, File dirOut) {
    try {
      byte[] iv = new byte[ENCRYPTION_IV_LENGTH];
      read(fileIn, iv);
      SecretKey key = EncryptionHelper.generateSymmetricKeyFromPassword("PASSWORD");
      final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      ZipInputStream zin = new ZipInputStream(new CipherInputStream(fileIn, cipher));
      ZipEntry ze;
      while ((ze = zin.getNextEntry()) != null) {
        Timber.v("Unzipping %s", ze.getName());
        File newFile = new File(dirOut, ze.getName());
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
      return true;
    } catch (IOException | GeneralSecurityException e) {
      Timber.w(e);
      return false;
    }
  }

  public static int read(InputStream in, byte[] b)
      throws IOException {
    int total = 0;
    while (total < b.length) {
      int result = in.read(b, total, b.length - total);
      if (result == -1) {
        break;
      }
      total += result;
    }
    return total;
  }
}