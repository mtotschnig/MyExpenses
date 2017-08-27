package org.totschnig.myexpenses.util;

public class NougatFileProviderException extends IllegalStateException {
  NougatFileProviderException(Throwable cause) {
    super("On Nougat, falling back to file uri won't work", cause);
  }
}
