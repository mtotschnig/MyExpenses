package org.totschnig.myexpenses.test.model;

import android.test.ProviderTestCase2;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.TransactionProvider;

public abstract class ModelTest extends ProviderTestCase2<TransactionProvider> {
  public ModelTest() {
    super(TransactionProvider.class, TransactionProvider.AUTHORITY);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Account.clear();
    Model.setContentResolver(getMockContentResolver());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Account.clear();
    PaymentMethod.clear();
  }
}
