package org.totschnig.myexpenses;

import android.support.annotation.NonNull;

import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.di.DaggerAppComponent;
import org.totschnig.myexpenses.di.UiModule;
import org.totschnig.myexpenses.testutils.MockAppModule;

public class TestMyApplication extends MyApplication {
  @NonNull
  @Override
  protected AppComponent buildAppComponent() {
    return DaggerAppComponent.builder()
        .appModule(new MockAppModule(this))
        .uiModule(new UiModule())
        .build();
  }

  @Override
  public void setupLogging() {

  }
}
