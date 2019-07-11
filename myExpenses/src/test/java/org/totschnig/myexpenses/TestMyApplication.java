package org.totschnig.myexpenses;

import androidx.annotation.NonNull;

import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.di.DaggerAppComponent;
import org.totschnig.myexpenses.testutils.MockLicenceModule;

public class TestMyApplication extends MyApplication {
  @NonNull
  @Override
  protected AppComponent buildAppComponent() {
    return DaggerAppComponent.builder()
        .licenceModule(new MockLicenceModule())
        .applicationContext(this)
        .build();
  }

  @Override
  public void setupLogging() {

  }
}
