package org.totschnig.myexpenses.test.model;

import org.totschnig.myexpenses.model.PaymentMethod;

public class PaymentMethodTest extends ModelTest  {
  
  public void testSavingAPredefinedMethodWithoutChangingLabelShouldKeppPredefinedInformation() {
    PaymentMethod pm = PaymentMethod.getInstanceFromDb(1);
    assertTrue(pm.isPredefined());
    pm.isNumbered = !pm.isNumbered;
    pm.save();
    PaymentMethod.clear();
    pm = PaymentMethod.getInstanceFromDb(1);
    assertTrue(pm.isPredefined());
  }
  
  public void testSavingAPredefinedMethodWithChangingLabelShouldDiscardPredefinedInformation() {
    PaymentMethod pm = PaymentMethod.getInstanceFromDb(1);
    assertTrue(pm.isPredefined());
    pm.setLabel("new label");
    pm.save();
    PaymentMethod.clear();
    pm = PaymentMethod.getInstanceFromDb(1);
    assertFalse(pm.isPredefined());
  }
}
