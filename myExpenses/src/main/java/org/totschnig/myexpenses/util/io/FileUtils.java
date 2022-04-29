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

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

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
  /**
   * Gets the extension of a file name, like ".png" or ".jpg".
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

        final int column_index = cursor.getColumnIndex(column);
        if (column_index == -1) {
          return null;
        }
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
   */
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

      if(AppDirHelper.getFileProviderAuthority(context).equals(uri.getAuthority())) {
        return PictureDirHelper.getFileForUri(uri).getPath();
      }

      // Return the remote address
      if (isGooglePhotosUri(uri))
        return uri.getLastPathSegment();

      dataColumn = getDataColumn(context, uri, null, null);
    }
    return dataColumn != null ? dataColumn : uri.getPath();
  }

  public static boolean isDocumentUri(Context context, Uri uri) {
    return DocumentsContract.isDocumentUri(context, uri);
  }

}