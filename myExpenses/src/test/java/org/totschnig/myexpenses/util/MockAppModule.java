package org.totschnig.myexpenses.util;

import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.ValidationException;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppModule;

class MockAppModule extends AppModule {
  MockAppModule(MyApplication testApplication) {
    super(testApplication);
  }

  @Override
  protected String provideDeviceId() {
    return "DUMMY";
  }

  @Override
  protected Obfuscator provideObfuscator(String deviceId) {
    return new Obfuscator() {
      @Override
      public String obfuscate(String original, String key) {
        return original;
      }

      @Override
      public String unobfuscate(String obfuscated, String key) throws ValidationException {
        return obfuscated;
      }
    };
  }
}
