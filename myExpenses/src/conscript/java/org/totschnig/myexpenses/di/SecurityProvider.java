package org.totschnig.myexpenses.di;

import android.os.Build;

import java.security.Security;

public class SecurityProvider {
  public static void init() {
    if (Build.VERSION.SDK_INT < 22) {
      Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);
    }
  }
}
