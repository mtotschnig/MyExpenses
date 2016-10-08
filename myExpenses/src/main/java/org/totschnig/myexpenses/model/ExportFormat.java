package org.totschnig.myexpenses.model;

import java.util.Locale;

public enum ExportFormat {
  QIF, CSV;

  public String getMimeType() {
    return "text/" + getExtension();
  }

  public String getExtension() {
    return name().toLowerCase(Locale.US);
  }
}
