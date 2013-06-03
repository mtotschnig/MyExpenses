package org.totschnig.myexpenses.test;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.Utils;

import junit.framework.Assert;
import android.content.Context;

public class ContribFeatureTest extends android.test.InstrumentationTestCase {
  Context ctx;
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      ctx = getInstrumentation().getTargetContext();
  }
  public void testFormattedList() {
    Assert.assertNotNull(Utils.getContribFeatureLabelsAsFormattedList(ctx));
  }
  public void testRecordUsage() {
    ContribFeature feature = ContribFeature.AGGREGATE;
    Assert.assertEquals(5,feature.usagesLeft());
    MyApplication.getInstance().isContribEnabled = false;
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    MyApplication.getInstance().isContribEnabled = true;
    feature.recordUsage();
    Assert.assertEquals(4,feature.usagesLeft());
    MyApplication.getInstance().isContribEnabled = false;
    feature.recordUsage();
    Assert.assertEquals(3,feature.usagesLeft());
  }
}
