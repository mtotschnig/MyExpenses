package org.totschnig.myexpenses.util;

import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import org.totschnig.myexpenses.MyApplication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PictureDirHelper {
  /**
   * create a File object for storage of picture data
   *
   * @param temp if true the returned file is suitable for temporary storage while
   *             the user is editing the transaction if false the file will serve
   *             as permanent storage,
   *             care is taken that the file does not yet exist
   * @return a file on the external storage
   */
  public static File getOutputMediaFile(String fileName, boolean temp) {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = temp ? Utils.getCacheDir() : getPictureDir();
    if (mediaStorageDir == null) return null;
    int postfix = 0;
    File result;
    do {
      result = new File(mediaStorageDir, getOutputMediaFileName(
          fileName,
          postfix));
      postfix++;
    } while (result.exists());
    return result;
  }

  public static Uri getOutputMediaUri(boolean temp) {
    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(new Date());
    File outputMediaFile;
    if (MyApplication.getInstance().isProtected() && !temp) {
      outputMediaFile = getOutputMediaFile(fileName, false);
      if (outputMediaFile == null) return null;
      return FileProvider.getUriForFile(MyApplication.getInstance(),
          MyApplication.getInstance().getPackageName() +".fileprovider",
          outputMediaFile);
    } else {
      outputMediaFile = getOutputMediaFile(fileName, temp);
      if (outputMediaFile == null) return null;
      return Uri.fromFile(outputMediaFile);
    }
  }

  public static String getPictureUriBase(boolean temp) {
    Uri sampleUri = getOutputMediaUri(temp);
    if (sampleUri == null) return null;
    String uriString = sampleUri.toString();
    return uriString.substring(0, uriString.lastIndexOf('/'));
  }

  private static String getOutputMediaFileName(String base, int postfix) {
    if (postfix > 0) {
      base += "_" + postfix;
    }
    return base + ".jpg";
  }

  public static File getPictureDir() {
    return getPictureDir(MyApplication.getInstance().isProtected());
  }

  public static File getPictureDir(boolean secure) {
    File result;
    if (secure) {
      result = new File(MyApplication.getInstance().getFilesDir(),
          "images");
    } else {
      result = MyApplication.getInstance().getExternalFilesDir(
          Environment.DIRECTORY_PICTURES);
    }
    if (result == null) return null;
    result.mkdir();
    return result.exists() ? result : null;
  }
}
