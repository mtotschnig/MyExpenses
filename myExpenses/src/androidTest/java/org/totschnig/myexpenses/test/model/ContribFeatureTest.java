package org.totschnig.myexpenses.test.model;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.FakeLicenceHandler;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Utils;

public class ContribFeatureTest extends ModelTest  {
  
  public void testFormattedList() {
    Assert.assertNotNull(Utils.getContribFeatureLabelsAsFormattedList(getContext(), null));
  }
  public void testRecordUsage() {
    ContribFeature feature = ContribFeature.ATTACH_PICTURE;
    MyApplication app = (MyApplication) getContext().getApplicationContext();
    FakeLicenceHandler licenceHandler = ((FakeLicenceHandler) app.getLicenceHandler());
    Assert.assertEquals(5,feature.usagesLeft());
    licenceHandler.setLicenceStatus(null);
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    licenceHandler.setLicenceStatus(LicenceHandler.LicenceStatus.CONTRIB);
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    licenceHandler.setLicenceStatus(null);
    feature.recordUsage();
    Assert.assertEquals(3,feature.usagesLeft());
  }
}
