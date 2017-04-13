package org.totschnig.myexpenses.di;


import org.totschnig.myexpenses.util.CurrencyFormatter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UtilsModule {

  @Provides
  @Singleton
  CurrencyFormatter provideCurrencyFormatter() {
    return CurrencyFormatter.instance();
  }
}
