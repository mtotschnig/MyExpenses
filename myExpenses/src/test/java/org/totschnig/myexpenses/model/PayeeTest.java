package org.totschnig.myexpenses.model;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayeeTest {

  Payee payee;

  @Before
  public void setupPayee() {
    payee = new Payee(0, "Anna");
    payee.save();
    Assertions.assertThat(payee.getId()).isGreaterThan(0);
  }

  @Test
  public void requireNewPayee() {
    final Long id = Payee.require("Petra");
    Assertions.assertThat(id).isGreaterThan(0);
    Assertions.assertThat(id).isNotEqualTo(payee.getId());
  }

  @Test
  public void requireNewPayeeWithSpace() {
    final Long id = Payee.require(" Petra ");
    Assertions.assertThat(id).isGreaterThan(0);
    Assertions.assertThat(id).isNotEqualTo(payee.getId());
    Assertions.assertThat(id).isEqualTo(Payee.require("Petra"));
  }

  @Test
  public void requireExistingPayee() {
    final Long id = Payee.require("Anna");
    Assertions.assertThat(id).isEqualTo(payee.getId());
  }

  @Test
  public void requireExistingPayeeWithSpace() {
    final Long id = Payee.require(" Anna ");
    Assertions.assertThat(id).isEqualTo(payee.getId());
  }
}
