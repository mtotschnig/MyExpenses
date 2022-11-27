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
import android.net.Uri;
import android.provider.DocumentsContract;

public class FileUtils {
  private FileUtils() {
  } //private constructor to enforce Singleton pattern

  // Set to true to enable logging

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

  public static boolean isDocumentUri(Context context, Uri uri) {
    return DocumentsContract.isDocumentUri(context, uri);
  }

}