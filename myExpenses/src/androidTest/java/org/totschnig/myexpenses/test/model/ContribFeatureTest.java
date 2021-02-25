package org.totschnig.myexpenses.test.model;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import static org.totschnig.myexpenses.model.ContribFeature.USAGES_LIMIT;

public class ContribFeatureTest extends ModelTest {

  public void testRecordUsage() {
    ContribFeature feature = ContribFeature.ATTACH_PICTURE;
    final AppComponent appComponent = ((MyApplication) getContext().getApplicationContext()).getAppComponent();
    LicenceHandler licenceHandler = appComponent.licenceHandler();
    PrefHandler prefHandler = appComponent.prefHandler();
    Assert.assertEquals(USAGES_LIMIT, feature.usagesLeft(prefHandler));
    licenceHandler.setLockState(true);
    feature.recordUsage(prefHandler, licenceHandler);
    Assert.assertEquals(USAGES_LIMIT - 1, feature.usagesLeft(prefHandler));
    licenceHandler.setLockState(false);
    feature.recordUsage(prefHandler, licenceHandler);
    Assert.assertEquals(USAGES_LIMIT - 1, feature.usagesLeft(prefHandler));
    licenceHandler.setLockState(true);
    feature.recordUsage(prefHandler, licenceHandler);
    Assert.assertEquals(USAGES_LIMIT - 2, feature.usagesLeft(prefHandler));
  }
}
