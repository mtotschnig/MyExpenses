package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;
import org.totschnig.myexpenses.util.ads.NoOpAdHandler;
import org.totschnig.myexpenses.util.ads.PubNativeAdHandler;

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
  AdHandlerFactory provideAdHandlerFactory(MyApplication application) {
    try {
      return (AdHandlerFactory) Class.forName(
          "org.totschnig.myexpenses.util.ads.PlatformAdHandlerFactory").newInstance();
    } catch (Exception e) {
      return (!AdHandler.isAdDisabled(application) &&
          Utils.isComAndroidVendingInstalled(application)) ?
          PubNativeAdHandler::new :
          NoOpAdHandler::new;
    }
  }

}
