package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UiModule {
  @Provides
  @Singleton
  ImageViewIntentProvider provideImageViewIntentProvider() {
    try {
      return (ImageViewIntentProvider) Class.forName(
          "org.totschnig.myexpenses.activity.PlatformImageViewIntentProvider").newInstance();
    } catch (Exception e) {
      return new SystemImageViewIntentProvider();
    }
  }

  @Provides
  @Singleton
  AdHandlerFactory provideAdHandlerFactory() {
    try {
      return (AdHandlerFactory) Class.forName(
          "org.totschnig.myexpenses.util.ads.PlatformAdHandlerFactory").newInstance();
    } catch (Exception e) {
      return adContainer -> new AdHandler(adContainer) {
        public void init() {
          hide();
        }
      };
    }
  }
}
