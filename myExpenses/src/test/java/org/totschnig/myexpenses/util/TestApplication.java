package org.totschnig.myexpenses.util;

import android.support.annotation.NonNull;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.di.DaggerAppComponent;
import org.totschnig.myexpenses.di.UiModule;

class TestApplication extends MyApplication {
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
