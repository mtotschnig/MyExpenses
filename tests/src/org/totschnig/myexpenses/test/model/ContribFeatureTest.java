package org.totschnig.myexpenses.test.model;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.Utils;

import junit.framework.Assert;

public class ContribFeatureTest extends ModelTest  {
  
  public void testFormattedList() {
    Assert.assertNotNull(Utils.getContribFeatureLabelsAsFormattedList(getContext()));
  }
  public void testRecordUsage() {
    Feature feature = Feature.AGGREGATE;
    MyApplication app = (MyApplication) getContext().getApplicationContext();
    Assert.assertEquals(5,feature.usagesLeft());
    app.isContribEnabled = false;
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    app.isContribEnabled = true;
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    app.isContribEnabled = false;
    feature.recordUsage();
    Assert.assertEquals(3,feature.usagesLeft());
  }
}
