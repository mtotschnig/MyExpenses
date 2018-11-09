package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;
import org.totschnig.myexpenses.util.ads.DefaultAdHandlerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UiModule {
  @Provides
  @Singleton
  ImageViewIntentProvider provideImageViewIntentProvider() {
      return new SystemImageViewIntentProvider();
  }

  @Provides
  @Singleton
  AdHandlerFactory provideAdHandlerFactory(MyApplication application, PrefHandler prefHandler, @Named("userCountry") String userCountry) {
    return new DefaultAdHandlerFactory(application, prefHandler, userCountry) {
      @Override
      public boolean isAdDisabled() {
        return true;
      }
    };
  }
}
