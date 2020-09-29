package org.totschnig.myexpenses;

import androidx.annotation.NonNull;

import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.di.DaggerAppComponent;
import org.totschnig.myexpenses.testutils.MockLicenceModule;

import java.util.Locale;

public class TestMyApplication extends MyApplication {
  @NonNull
  @Override
  protected AppComponent buildAppComponent(Locale systemLocale) {
    return DaggerAppComponent.builder()
        .licenceModule(new MockLicenceModule())
        .applicationContext(this)
        .systemLocale(systemLocale)
        .build();
  }

  @Override
  public void setupLogging() {

  }
}
