package org.totschnig.myexpenses.util;

import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppDirHelper {
  /**
   * @return the directory user has configured in the settings, if not configured yet
   * returns {@link android.content.ContextWrapper#getExternalFilesDir(String)} with argument null
   */
  public static DocumentFile getAppDir() {
    String prefString = PrefKey.APP_DIR.getString(null);
    if (prefString != null) {
      Uri pref = Uri.parse(prefString);
      if (pref.getScheme().equals("file")) {
        File appDir = new File(pref.getPath());
        if (appDir.mkdir() || appDir.isDirectory()) {
          return DocumentFile.fromFile(appDir);
        }/* else {
          Utils.reportToAcra(new Exception("Found invalid preference value " + prefString));
        }*/
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          //this will return null, if called on a pre-Lolipop device
          DocumentFile documentFile = DocumentFile.fromTreeUri(MyApplication.getInstance(), pref);
          if (dirExistsAndIsWritable(documentFile)) {
            return documentFile;
          }
        }
      }
    }
    File externalFilesDir = MyApplication.getInstance().getExternalFilesDir(null);
    if (externalFilesDir != null) {
      return DocumentFile.fromFile(externalFilesDir);
    } else {
      String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
      AcraHelper.report(new Exception("getExternalFilesDir returned null; " + permission + " : " +
          ContextCompat.checkSelfPermission(MyApplication.getInstance(), permission)));
      return null;
    }
  }

  public static File getCacheDir() {
    File external = MyApplication.getInstance().getExternalCacheDir();
    return external != null ? external : MyApplication.getInstance()
        .getCacheDir();
  }

  /**
   * @param parentDir
   * @param prefix
   * @param addExtension
   * @return creates a file object in parentDir, with a timestamp appended to
   * prefix as name, if the file already exists it appends a numeric
   * postfix
   */
  public static DocumentFile timeStampedFile(DocumentFile parentDir, String prefix,
                                             String mimeType, boolean addExtension) {
    String now = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        .format(new Date());
    return newFile(parentDir, prefix + "-" + now, mimeType, addExtension);
  }

  public static DocumentFile newFile(DocumentFile parentDir, String base,
                                     String mimeType, boolean addExtension) {
    int postfix = 0;
    do {
      String name = base;
      if (postfix > 0) {
        name += "_" + postfix;
      }
      if (addExtension) {
        name += "." + mimeType.split("/")[1];
      }
      if (parentDir.findFile(name) == null) {
        DocumentFile result = null;
        try {
          result = parentDir.createFile(mimeType, name);
          if (result == null) {
            AcraHelper.report(new Exception(String.format(
                "createFile returned null: mimeType %s; name %s; parent %s",
                mimeType, name, parentDir.getUri().toString())));
          }
        } catch (SecurityException e) {
          AcraHelper.report(new Exception(String.format(
              "createFile threw SecurityException: mimeType %s; name %s; parent %s",
              mimeType, name, parentDir.getUri().toString())));
        }
        return result;
      }
      postfix++;
    } while (true);
  }

  public static DocumentFile newDirectory(DocumentFile parentDir, String base) {
    int postfix = 0;
    do {
      String name = base;
      if (postfix > 0) {
        name += "_" + postfix;
      }
      if (parentDir.findFile(name) == null) {
        return parentDir.createDirectory(name);
      }
      postfix++;
    } while (true);
  }

  /**
   * Helper Method to Test if external Storage is Available from
   * http://www.ibm.com/developerworks/xml/library/x-androidstorage/index.html
   */
  public static boolean isExternalStorageAvailable() {
    boolean state = false;
    String extStorageState = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
      state = true;
    }
    return state;
  }

  public static Result checkAppDir() {
    if (!isExternalStorageAvailable()) {
      return new Result(false, R.string.external_storage_unavailable);
    }
    DocumentFile appDir = getAppDir();
    if (appDir == null) {
      return new Result(false, R.string.io_error_appdir_null);
    }
    return dirExistsAndIsWritable(appDir) ?
        new Result(true) : new Result(false, R.string.app_dir_not_accessible,
        FileUtils.getPath(MyApplication.getInstance(), appDir.getUri()));
  }

  @NonNull
  public static boolean dirExistsAndIsWritable(DocumentFile appdir) {
    return appdir.exists() && appdir.canWrite();
  }

  public static Uri ensureContentUri(Uri uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      switch (uri.getScheme()) {
        case "file":
          try {
            uri = getContentUriForFile(new File(uri.getPath()));
          } catch (IllegalArgumentException e) {
            AcraHelper.report(e);
          }
          break;
        case "content":
          break;
        default:
          AcraHelper.report(new IllegalStateException(String.format(
              "Unable to handle scheme of uri %s", uri)));
      }
    }
    return uri;
  }

  static Uri getContentUriForFile(File file) {
    return FileProvider.getUriForFile(MyApplication.getInstance(),
        MyApplication.getInstance().getPackageName() + ".fileprovider",
        file);
  }

  /**
   * @return false if the configured folder is inside the application folder
   * that will be deleted upon app uninstall and hence user should be
   * warned about the situation, unless he already has opted to no
   * longer see this warning
   */
  public static boolean checkAppFolderWarning() {
    if (PrefKey.APP_FOLDER_WARNING_SHOWN.getBoolean(false)) {
      return true;
    }
    try {
      DocumentFile configuredDir = getAppDir();
      if (configuredDir == null) {
        return true;
      }
      File externalFilesDir = MyApplication.getInstance().getExternalFilesDir(
          null);
      if (externalFilesDir == null) {
        return true;
      }
      Uri dirUri = configuredDir.getUri();
      if (!dirUri.getScheme().equals("file")) {
        return true; //nothing we can do if we can not compare paths
      }
      URI defaultDir = externalFilesDir.getParentFile().getCanonicalFile()
          .toURI();
      return defaultDir.relativize(new File(dirUri.getPath()).getCanonicalFile().toURI())
          .isAbsolute();
    } catch (IOException e) {
      return true;
    }
  }
}
