package org.totschnig.myexpenses.test.model;

import org.totschnig.myexpenses.model.PaymentMethod;

public class PaymentMethodTest extends ModelTest {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PaymentMethod.clear();
  }

  public void testSavingAPredefinedMethodWithoutChangingLabelShouldKeepPredefinedInformation() {
    PaymentMethod pm = PaymentMethod.getInstanceFromDb(1);
    assert pm != null;
    assertTrue(isPredefined(pm));
    pm.isNumbered = !pm.isNumbered;
    pm.save();
    PaymentMethod.clear();
    pm = PaymentMethod.getInstanceFromDb(1);
    assert pm != null;
    assertTrue(isPredefined(pm));
    //cleanup
    pm.isNumbered = !pm.isNumbered;
    pm.save();
  }

  public void testSavingAPredefinedMethodWithChangingLabelShouldDiscardPredefinedInformation() {
    PaymentMethod pm = PaymentMethod.getInstanceFromDb(1);
    assert pm != null;
    assertTrue(isPredefined(pm));
    PaymentMethod.PreDefined origPredefined = pm.getPreDefined();
    pm.setLabel("new label");
    pm.save();
    PaymentMethod.clear();
    pm = PaymentMethod.getInstanceFromDb(1);
    assert pm != null;
    assertFalse(isPredefined(pm));
    //cleanup
    pm.setLabel(origPredefined.name());
    pm.save();
  }

  private boolean isPredefined(PaymentMethod pm) {
    return pm.getPreDefined() != null;
  }
}
