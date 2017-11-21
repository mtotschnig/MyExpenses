package org.totschnig.myexpenses.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.totschnig.myexpenses.testutils.TestApplication;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class, packageName = "org.totschnig.myexpenses")
public class ContribFeatureTest {

  @Test
  public void labelsShouldBeDefinedAsResources() {
    for (ContribFeature contribFeature : ContribFeature.values()) {
      assertThat(contribFeature.getLabelResId(RuntimeEnvironment.application))
          .withFailMessage("label for " + contribFeature + " not defined").isGreaterThan(0);
    }
  }
}
