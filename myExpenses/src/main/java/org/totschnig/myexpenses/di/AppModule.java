package org.totschnig.myexpenses.di;

import org.totschnig.myexpenses.util.AcraWrapperIFace;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.LicenceHandlerIFace;
import org.totschnig.myexpenses.util.NoopAcraWrapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

  @Provides
  @Singleton
  LicenceHandlerIFace providesLicenceHandler() {
    return new LicenceHandler();
  }
  @Provides
  @Singleton
  AcraWrapperIFace providesAcraWrapper() {
    return new NoopAcraWrapper();
  }
}
