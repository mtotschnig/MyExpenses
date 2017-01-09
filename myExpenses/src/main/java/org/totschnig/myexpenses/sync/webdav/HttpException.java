/*
 * Copyright 2016 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.myexpenses.sync.webdav;

import android.annotation.SuppressLint;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public class HttpException extends Exception {
  private int mCode;

  HttpException(Response response) {
    this(response.request(), response.code(), response.message(), null);
  }

  HttpException(Request request, IOException exception) {
    this(request, -1, "Connection failed: " + exception.getMessage(), exception);
  }

  HttpException(Exception e) {
    super(e);
    mCode = -1;
  }

  @SuppressLint("DefaultLocale")
  private HttpException(Request request, int code, String message, Exception innerException) {
    super(String.format(
        "Error connecting to the WebDAV server: %d %s. Was trying to execute request: %s %s",
        code, message, request.method(), request.url().toString()),
        innerException);
    mCode = code;
  }

  public boolean isNetworkIssue() {
    return mCode == -1;
  }

  public boolean isUnauthorized() {
    return mCode == 401;
  }

  public boolean isNotFound() {
    return mCode == 404;
  }
}
