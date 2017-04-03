package org.totschnig.myexpenses.test.model;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.LicenceHandler;

import static org.totschnig.myexpenses.model.ContribFeature.USAGES_LIMIT;

public class ContribFeatureTest extends ModelTest  {

  public void testRecordUsage() {
    ContribFeature feature = ContribFeature.ATTACH_PICTURE;
    MyApplication app = (MyApplication) getContext().getApplicationContext();
    LicenceHandler licenceHandler = app.getLicenceHandler();
    Assert.assertEquals(USAGES_LIMIT,feature.usagesLeft());
    licenceHandler.setLockState(true);
    feature.recordUsage();
    Assert.assertEquals(USAGES_LIMIT - 1,feature.usagesLeft());
    licenceHandler.setLockState(false);
    feature.recordUsage();
    Assert.assertEquals(USAGES_LIMIT - 1,feature.usagesLeft());
    licenceHandler.setLockState(true);
    feature.recordUsage();
    Assert.assertEquals(USAGES_LIMIT - 2,feature.usagesLeft());
  }
}
