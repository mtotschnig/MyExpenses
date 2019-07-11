package org.totschnig.myexpenses.util;

import androidx.annotation.NonNull;

public class StringBuilderWrapper {
  public StringBuilderWrapper() {
    this.sb = new StringBuilder();
  }

  private StringBuilder sb;

  public StringBuilderWrapper append(String s) {
    sb.append(s);
    return this;
  }

  public StringBuilderWrapper append(char c) {
    sb.append(c);
    return this;
  }

  public StringBuilderWrapper appendQ(@NonNull String s) {
    sb.append('"').append(s.replace("\"", "\"\"")).append('"');
    return this;
  }

  @NonNull
  public String toString() {
    return sb.toString();
  }

  public void clear() {
    sb = new StringBuilder();
  }
}
