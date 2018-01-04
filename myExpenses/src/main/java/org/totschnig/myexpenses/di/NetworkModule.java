package org.totschnig.myexpenses.di;


import org.totschnig.myexpenses.BuildConfig;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.logging.HttpLoggingInterceptor;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BASIC;
import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

@Module
public class NetworkModule {

  @Provides
  @Singleton
  HttpLoggingInterceptor provideHttpLoggingInterceptor() {
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(BuildConfig.DEBUG ? BODY : BASIC);
    return loggingInterceptor;
  }
}
