package org.totschnig.myexpenses.test.model;

import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.testutils.BaseProviderTest;

public abstract class ModelTest extends BaseProviderTest {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Model.setContentResolver(getMockContentResolver());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    PaymentMethod.clear();
  }
}
