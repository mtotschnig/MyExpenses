package org.totschnig.myexpenses.di;

import android.content.Context;
import android.os.Build;

import java.security.Security;

public class SecurityProvider {
  public static void init(Context context) {
    if (Build.VERSION.SDK_INT < 22) {
      Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);
    }
  }
}
