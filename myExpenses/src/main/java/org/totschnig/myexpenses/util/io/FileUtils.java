/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java

package org.totschnig.myexpenses.util.io;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.File;
import java.text.DecimalFormat;

import timber.log.Timber;

/**
 * @author Peli
 * @author paulburke (ipaulpro)
 * @version 2013-12-11
 */
public class FileUtils {
  private FileUtils() {
  } //private constructor to enforce Singleton pattern

  private static final boolean DEBUG = BuildConfig.DEBUG; // Set to true to enable logging

  public static final String MIME_TYPE_AUDIO = "audio/*";
  public static final String MIME_TYPE_TEXT = "text/*";
  public static final String MIME_TYPE_IMAGE = "image/*";
  public static final String MIME_TYPE_VIDEO = "video/*";
  public static final String MIME_TYPE_APP = "application/*";

  public static final String HIDDEN_PREFIX = ".";

  /**
   * Gets the extension of a file name, like ".png" or ".jpg".
   *
   * @param uri
   * @return Extension including the dot("."); "" if there is no extension;
   * null if uri was null.
   */
  public static String getExtension(String uri) {
    if (uri == null) {
      return null;
    }

    int dot = uri.lastIndexOf(".");
    if (dot >= 0) {
      return uri.substring(dot);
    } else {
      // No extension.
      return "";
    }
  }

  /**
   * @return Whether the URI is a local one.
   */
  public static boolean isLocal(String url) {
    if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
      return true;
    }
    return false;
  }

  /**
   * @return True if Uri is a MediaStore Uri.
   * @author paulburke
   */
  public static boolean isMediaUri(Uri uri) {
    return "media".equalsIgnoreCase(uri.getAuthority());
  }

  /**
   * Convert File into Uri.
   *
   * @param file
   * @return uri
   */
  public static Uri getUri(File file) {
    if (file != null) {
      return Uri.fromFile(file);
    }
    return null;
  }

  /**
   * Returns the path only (without file name).
   *
   * @param file
   * @return
   */
  public static File getPathWithoutFilename(File file) {
    if (file != null) {
      if (file.isDirectory()) {
        // no file to be split off. Return everything
        return file;
      } else {
        String filename = file.getName();
        String filepath = file.getAbsolutePath();

        // Construct path without file name.
        String pathwithoutname = filepath.substring(0,
            filepath.length() - filename.length());
        if (pathwithoutname.endsWith("/")) {
          pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length() - 1);
        }
        return new File(pathwithoutname);
      }
    }
    return null;
  }

  /**
   * @return The MIME type for the given file.
   */
  public static String getMimeType(File file) {

    String extension = getExtension(file.getName());

    if (extension.length() > 0)
      return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));

    return "application/octet-stream";
  }

  /**
   * @return The MIME type for the give Uri.
   */
  public static String getMimeType(Context context, Uri uri) {
    File file = new File(getPath(context, uri));
    return getMimeType(file);
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   * @author paulburke
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   * @author paulburke
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   * @author paulburke
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   */
  public static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context       The context.
   * @param uri           The Uri to query.
   * @param selection     (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   * @author paulburke
   */
  public static String getDataColumn(Context context, Uri uri, String selection,
                                     String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
        column
    };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
          null);
      if (cursor != null && cursor.moveToFirst()) {
        if (DEBUG)
          DatabaseUtils.dumpCursor(cursor);

        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } catch (Exception e) {
      CrashHandler.report(e);
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   * Get a file path from a Uri. This will get the the path for Storage Access
   * Framework Documents, as well as the _data field for the MediaStore and
   * other file-based ContentProviders.<br>
   * <br>
   * Callers should only use this for display purposes and not for accessing the file directly via the
   * file system
   *
   * @param context The context.
   * @param uri     The Uri to query.
   * @author paulburke
   * @see #isLocal(String)
   * @see #getFile(Context, Uri)
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static String getPath(final Context context, final Uri uri) {
    Timber.d("Authority: %s, Fragment: %s, Port: %s, Query: %s, Scheme: %s, Host: %s, Segments: %s",
        uri.getAuthority(), uri.getFragment(), uri.getPort(), uri.getQuery(), uri.getScheme(), uri.getHost(),
        uri.getPathSegments().toString());
    
    String dataColumn = null;

    // DocumentProvider
    if (isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          String path = Environment.getExternalStorageDirectory().getPath();
          if (split.length > 1) {
            path += "/" + split[1];
          }
          return path;
        }
        //there is no documented way of returning a path to a file on non primary storage.
        //so what we do is displaying the documentId to the user which is better than just null
        return docId;
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String docId = DocumentsContract.getDocumentId(uri);
        final Uri contentUri;
        try {
          contentUri = ContentUris.withAppendedId(
              Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
          dataColumn = getDataColumn(context, contentUri, null, null);
        } catch (NumberFormatException e) {
          final String[] split = docId.split(":");
          if (split.length > 1) {
            return split[1];
          }
        }
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{
            split[1]
        };

        dataColumn = getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {

      if(AppDirHelper.getFileProviderAuthority().equals(uri.getAuthority())) {
        return PictureDirHelper.getFileForUri(uri).getPath();
      }

      // Return the remote address
      if (isGooglePhotosUri(uri))
        return uri.getLastPathSegment();

      dataColumn = getDataColumn(context, uri, null, null);
    }
    return dataColumn != null ? dataColumn : uri.getPath();
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static boolean isDocumentUri(Context context, Uri uri) {
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    return isKitKat && DocumentsContract.isDocumentUri(context, uri);
  }

  /**
   * Convert Uri into File, if possible.
   *
   * @return file A local file that the Uri was pointing to, or null if the
   * Uri is unsupported or pointed to a remote resource.
   * @author paulburke
   * @see #getPath(Context, Uri)
   */
  public static File getFile(Context context, Uri uri) {
    if (uri != null) {
      String path = getPath(context, uri);
      if (path != null && isLocal(path)) {
        return new File(path);
      }
    }
    return null;
  }

  /**
   * Get the file size in a human-readable string.
   *
   * @param size
   * @return
   * @author paulburke
   */
  public static String getReadableFileSize(int size) {
    final int BYTES_IN_KILOBYTES = 1024;
    final DecimalFormat dec = new DecimalFormat("###.#");
    final String KILOBYTES = " KB";
    final String MEGABYTES = " MB";
    final String GIGABYTES = " GB";
    float fileSize = 0;
    String suffix = KILOBYTES;

    if (size > BYTES_IN_KILOBYTES) {
      fileSize = size / BYTES_IN_KILOBYTES;
      if (fileSize > BYTES_IN_KILOBYTES) {
        fileSize = fileSize / BYTES_IN_KILOBYTES;
        if (fileSize > BYTES_IN_KILOBYTES) {
          fileSize = fileSize / BYTES_IN_KILOBYTES;
          suffix = GIGABYTES;
        } else {
          suffix = MEGABYTES;
        }
      }
    }
    return String.valueOf(dec.format(fileSize) + suffix);
  }

  /**
   * Attempt to retrieve the thumbnail of given File from the MediaStore. This
   * should not be called on the UI thread.
   *
   * @param context
   * @param file
   * @return
   * @author paulburke
   */
  public static Bitmap getThumbnail(Context context, File file) {
    return getThumbnail(context, getUri(file), getMimeType(file));
  }

  /**
   * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
   * should not be called on the UI thread.
   *
   * @param context
   * @param uri
   * @return
   * @author paulburke
   */
  public static Bitmap getThumbnail(Context context, Uri uri) {
    return getThumbnail(context, uri, getMimeType(context, uri));
  }

  /**
   * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
   * should not be called on the UI thread.
   *
   * @param context
   * @param uri
   * @param mimeType
   * @return
   * @author paulburke
   */
  public static Bitmap getThumbnail(Context context, Uri uri, String mimeType) {
    Timber.d("Attempting to get thumbnail");

    if (!isMediaUri(uri)) {
      CrashHandler.report("You can only retrieve thumbnails for images and videos.");
      return null;
    }

    Bitmap bm = null;
    if (uri != null) {
      final ContentResolver resolver = context.getContentResolver();
      try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
        if (cursor.moveToFirst()) {
          final int id = cursor.getInt(0);
          Timber.d("Got thumb ID: %d", id);

          if (mimeType.contains("video")) {
            bm = MediaStore.Video.Thumbnails.getThumbnail(
                resolver,
                id,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null);
          } else if (mimeType.contains(FileUtils.MIME_TYPE_IMAGE)) {
            bm = MediaStore.Images.Thumbnails.getThumbnail(
                resolver,
                id,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null);
          }
        }
      } catch (Exception e) {
        Timber.e(e, "getThumbnail");
      }
    }
    return bm;
  }
}