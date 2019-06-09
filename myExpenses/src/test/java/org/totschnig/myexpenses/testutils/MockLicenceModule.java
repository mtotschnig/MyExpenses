package org.totschnig.myexpenses.testutils;

import com.google.android.vending.licensing.Obfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.LicenceModule;

public class MockLicenceModule extends LicenceModule {

  @Override
  protected String provideDeviceId(MyApplication application) {
    return "DUMMY";
  }

  @Override
  protected Obfuscator provideObfuscator(String deviceId, MyApplication application) {
    return new Obfuscator() {
      @Override
      public String obfuscate(String original, String key) {
        return original;
      }

      @Override
      public String unobfuscate(String obfuscated, String key) {
        return obfuscated;
      }
    };
  }
}
