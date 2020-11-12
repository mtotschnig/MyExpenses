package org.totschnig.myexpenses.model;

import java.util.Locale;

public enum ExportFormat {
  QIF {
    @Override
    public String getMimeType() {
      return "application/qif";
    }
  }, CSV {
    @Override
    public String getMimeType() {
      return "text/csv";
    }
  };

  public abstract String getMimeType();

  public String getExtension() {
    return name().toLowerCase(Locale.US);
  }
}
