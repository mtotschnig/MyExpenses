package org.totschnig.myexpenses.util;

import org.junit.Test;
import org.totschnig.myexpenses.BuildConfig;

import static org.junit.Assert.*;

public class DistribHelperTest {

  @Test
  public void distributionShouldBeTakenUpFromBuildConfig() {
    assertEquals(DistribHelper.getDistributionAsString(), BuildConfig.DISTRIBUTION);
  }
}
