package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.activity.ImageViewIntentProvider;
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider;

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
}
