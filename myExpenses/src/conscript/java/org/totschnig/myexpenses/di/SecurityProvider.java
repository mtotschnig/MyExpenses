package org.totschnig.myexpenses.di;

import android.content.Context;
import android.os.Build;

import java.security.Security;

import timber.log.Timber;

public class SecurityProvider {
  public static void init(Context context) {
    if (Build.VERSION.SDK_INT < 22) {
      try {
        Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }
}
