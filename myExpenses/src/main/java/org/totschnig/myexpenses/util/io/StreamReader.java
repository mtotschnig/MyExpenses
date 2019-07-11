/*
 * Copyright (c) 2017 the ACRA team
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

package org.totschnig.myexpenses.util.io;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * From Acra
 * @author F43nd1r
 * @since 30.11.2017
 */

public class StreamReader {
  private static final int DEFAULT_BUFFER_SIZE_IN_BYTES = 8192;

  private static final int NO_LIMIT = -1;
  private static final int INDEFINITE = -1;
  private final InputStream inputStream;
  private int limit = NO_LIMIT;
  private int timeout = INDEFINITE;

  public StreamReader(@NonNull String filename) throws FileNotFoundException {
    this(new File(filename));
  }

  public StreamReader(@NonNull File file) throws FileNotFoundException {
    this(new FileInputStream(file));
  }

  public StreamReader(@NonNull InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @NonNull
  public StreamReader setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  @NonNull
  public StreamReader setTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  @NonNull
  public String read() throws IOException {
    final String text = timeout == INDEFINITE ? readFully() : readWithTimeout();
    if (limit == NO_LIMIT) {
      return text;
    }
    final String[] lines = text.split("\\r?\\n");
    if(lines.length <= limit){
      return text;
    }
    return TextUtils.join("\n", Arrays.copyOfRange(lines, lines.length - limit, lines.length));
  }

  @NonNull
  private String readFully() throws IOException {
    final Reader input = new InputStreamReader(inputStream);
    try {
      final StringWriter output = new StringWriter();
      final char[] buffer = new char[DEFAULT_BUFFER_SIZE_IN_BYTES];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toString();
    } finally {
      safeClose(input);
    }
  }

  @NonNull
  private String readWithTimeout() throws IOException {
    final long until = System.currentTimeMillis() + timeout;
    try {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE_IN_BYTES];
      int count;
      while ((count = fillBufferUntil(buffer, until)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toString();
    } finally {
      safeClose(inputStream);
    }
  }

  private int fillBufferUntil(@NonNull byte[] buffer, long until) throws IOException {
    int bufferOffset = 0;
    while (System.currentTimeMillis() < until && bufferOffset < buffer.length) {
      final int readResult = inputStream.read(buffer, bufferOffset, Math.min(inputStream.available(), buffer.length - bufferOffset));
      if (readResult == -1) break;
      bufferOffset += readResult;
    }
    return bufferOffset;
  }

  /**
   * Closes a Closeable.
   *
   * @param closeable Closeable to close. If closeable is null then method just returns.
   */
  public static void safeClose(@Nullable Closeable closeable) {
    if (closeable == null) return;

    try {
      closeable.close();
    } catch (IOException ignored) {
      // We made out best effort to release this resource. Nothing more we can do.
    }
  }
}
