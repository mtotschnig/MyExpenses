package org.totschnig.myexpenses.util;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

public class AppDirHelper {
  /**
   * @return the directory user has configured in the settings, if not configured yet
   * returns {@link android.content.ContextWrapper#getExternalFilesDir(String)} with argument null
   */
  @Nullable
  public static DocumentFile getAppDir(Context context) {
    String prefString = PrefKey.APP_DIR.getString(null);
    if (prefString != null) {
      Uri pref = Uri.parse(prefString);
      if ("file".equals(pref.getScheme())) {
        File appDir = new File(pref.getPath());
        if (appDir.mkdir() || appDir.isDirectory()) {
          return DocumentFile.fromFile(appDir);
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          //this will return null, if called on a pre-Lolipop device
          return DocumentFile.fromTreeUri(context, pref);
        }
      }
    }
    File externalFilesDir = context.getExternalFilesDir(null);
    if (externalFilesDir != null) {
      return DocumentFile.fromFile(externalFilesDir);
    } else {
      String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
      CrashHandler.report("getExternalFilesDir returned null; " + permission + " : " +
          PermissionHelper.getSelfPermissionSafe(context, permission));
      return null;
    }
  }

  public static File getCacheDir() {
    File external = MyApplication.getInstance().getExternalCacheDir();
    return (external != null && external.canWrite()) ? external : MyApplication.getInstance()
        .getCacheDir();
  }

  /**
   * @return creates a file object in parentDir, with a timestamp appended to
   * prefix as name, if the file already exists it appends a numeric
   * postfix
   */
  public static DocumentFile timeStampedFile(@NonNull DocumentFile parentDir, String prefix,
                                             String mimeType, String addExtension) {
    String now = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        .format(new Date());
    String name = prefix + "-" + now;
    if (addExtension != null) {
      name += "." + addExtension;
    }
    return buildFile(parentDir, name, mimeType, false, false);
  }

  @Nullable
  public static DocumentFile buildFile(@NonNull final DocumentFile parentDir, final String fileName,
                                       final String mimeType, final boolean allowExisting,
                                       final boolean supplementExtension) {
    //createFile supplements extension on known mimeTypes, if the mime type is not known, we take care of it
    String supplementedFilename = String.format(Locale.ROOT, "%s.%s", fileName, mimeType.split("/")[1]);
    String fileNameToCreate = fileName;
    if (supplementExtension) {
      final String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
      if (extensionFromMimeType == null) {
        fileNameToCreate = supplementedFilename;
      }
    }
    if (allowExisting) {
      DocumentFile existingFile = parentDir.findFile(supplementedFilename);
      if (existingFile != null) {
        return existingFile;
      }
    }
    DocumentFile result = null;
    try {
      result = parentDir.createFile(mimeType, fileNameToCreate);
      if (result == null || !result.canWrite()) {
        String message = result == null ? "createFile returned null" : "createFile returned unwritable file";
        Map<String, String> customData = new HashMap<>();
        customData.put("mimeType", mimeType);
        customData.put("name", fileName);
        customData.put("parent", parentDir.getUri().toString());
        CrashHandler.report(new Exception(message), customData);
      }
    } catch (SecurityException e) {
      CrashHandler.report(e);
    }
    return result;
  }

  @Nullable
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

  /**
   * Chechs is application directory is writable.
   * TODO: Should only be called from background
   * @param context activity or application
   * @return either positive Result or negative Result with problem description
   */
  public static Result checkAppDir(Context context) {
    if (!isExternalStorageAvailable()) {
      return Result.ofFailure(R.string.external_storage_unavailable);
    }
    DocumentFile appDir = getAppDir(context);
    if (appDir == null) {
      return Result.ofFailure(R.string.io_error_appdir_null);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Uri uri = appDir.getUri();
      if ("file".equals(uri.getScheme())) {
        try {
          getContentUriForFile(new File(new File(uri.getPath()), "test"));
        } catch (IllegalArgumentException e) {
          return Result.ofFailure(R.string.app_dir_not_compatible_with_nougat, uri.toString());
        }
      }
    }
    return isWritableDirectory(appDir) ? Result.SUCCESS : Result.ofFailure(
        R.string.app_dir_not_accessible, FileUtils.getPath(context, appDir.getUri()));
  }

  public static boolean isWritableDirectory(@NonNull DocumentFile appdir) {
    return appdir.exists() && appdir.isDirectory() && appdir.canWrite();
  }

  public static Uri ensureContentUri(Uri uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      switch (uri.getScheme()) {
        case "file":
          try {
            uri = getContentUriForFile(new File(uri.getPath()));
          } catch (IllegalArgumentException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              throw new NougatFileProviderException(e);
            }
          }
          break;
        case "content":
          break;
        default:
          CrashHandler.report(new IllegalStateException(String.format(
              "Unable to handle scheme of uri %s", uri)));
      }
    }
    return uri;
  }

  public static Uri getContentUriForFile(File file) {
    return FileProvider.getUriForFile(MyApplication.getInstance(),
        getFileProviderAuthority(),
        file);
  }

  @NonNull
  public static String getFileProviderAuthority() {
    return MyApplication.getInstance().getPackageName() + ".fileprovider";
  }

  /**
   * @return false if the configured folder is inside the application folder
   * that will be deleted upon app uninstall and hence user should be
   * warned about the situation, unless he already has opted to no
   * longer see this warning
   */
  public static boolean checkAppFolderWarning(Context context) {
    if (PrefKey.APP_FOLDER_WARNING_SHOWN.getBoolean(false)) {
      return true;
    }
    try {
      DocumentFile configuredDir = getAppDir(context);
      if (configuredDir == null) {
        return true;
      }
      File externalFilesDir =context.getExternalFilesDir(null);
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
