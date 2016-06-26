package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.MyApplication;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
  void inject(MyApplication application);
}
