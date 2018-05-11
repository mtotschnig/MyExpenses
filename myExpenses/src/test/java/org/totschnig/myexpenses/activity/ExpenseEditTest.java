package org.totschnig.myexpenses.activity;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;

import static org.totschnig.myexpenses.activity.ExpenseEdit.DateMode.BOOKING_VALUE;
import static org.totschnig.myexpenses.activity.ExpenseEdit.DateMode.DATE;
import static org.totschnig.myexpenses.activity.ExpenseEdit.DateMode.DATE_TIME;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_TIME;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_VALUE_DATE;

@RunWith(RobolectricTestRunner.class)
@Config(packageName = "org.totschnig.myexpenses")
public class ExpenseEditTest {
  private ExpenseEdit activity;
  private Account cashAccount, bankAcount;

  @Before
  public void setup() {
    activity = Robolectric.buildActivity(ExpenseEdit.class).get();
    activity.injectDependencies();
    cashAccount = Mockito.mock(Account.class);
    bankAcount = Mockito.mock(Account.class);
    Mockito.when(cashAccount.getType()).thenReturn(AccountType.CASH);
    Mockito.when(bankAcount.getType()).thenReturn(AccountType.BANK);
  }

  @After
  public void tearDown() {
    activity.finish();
    activity = null;
  }

  @Test
  public void withTimeAndWithValueDate() {
    activity.prefHandler.putBoolean(TRANSACTION_WITH_TIME, true);
    activity.prefHandler.putBoolean(TRANSACTION_WITH_VALUE_DATE, true);

    Assert.assertEquals(DATE_TIME, activity.getDateMode(cashAccount));
    Assert.assertEquals(BOOKING_VALUE, activity.getDateMode(bankAcount));
  }

  @Test
  public void withTimeAndWithoutValueDate() {
    activity.prefHandler.putBoolean(TRANSACTION_WITH_TIME, true);
    activity.prefHandler.putBoolean(TRANSACTION_WITH_VALUE_DATE, false);

    Assert.assertEquals(DATE_TIME, activity.getDateMode(cashAccount));
    Assert.assertEquals(DATE_TIME, activity.getDateMode(bankAcount));
  }


  @Test
  public void withoutTimeAndWithValueDate() {
    activity.prefHandler.putBoolean(TRANSACTION_WITH_TIME, false);
    activity.prefHandler.putBoolean(TRANSACTION_WITH_VALUE_DATE, true);

    Assert.assertEquals(DATE, activity.getDateMode(cashAccount));
    Assert.assertEquals(BOOKING_VALUE, activity.getDateMode(bankAcount));
  }

  @Test
  public void withoutTimeAndWithoutValueDate() {
    activity.prefHandler.putBoolean(TRANSACTION_WITH_TIME, false);
    activity.prefHandler.putBoolean(TRANSACTION_WITH_VALUE_DATE, false);

    Assert.assertEquals(DATE, activity.getDateMode(cashAccount));
    Assert.assertEquals(DATE, activity.getDateMode(bankAcount));
  }

}
