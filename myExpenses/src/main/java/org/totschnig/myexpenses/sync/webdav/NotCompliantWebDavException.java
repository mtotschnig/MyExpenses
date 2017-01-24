package org.totschnig.myexpenses.sync.webdav;

public class NotCompliantWebDavException extends Exception {
  public NotCompliantWebDavException(boolean fallbackToClass1) {
    this.fallbackToClass1 = fallbackToClass1;
  }

  private boolean fallbackToClass1 = false;

  public boolean isFallbackToClass1() {
    return fallbackToClass1;
  }
}
