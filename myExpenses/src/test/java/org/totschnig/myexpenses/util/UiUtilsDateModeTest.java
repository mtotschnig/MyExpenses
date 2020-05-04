package org.totschnig.myexpenses.util;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_TIME;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_VALUE_DATE;
import static org.totschnig.myexpenses.util.UiUtils.DateMode.BOOKING_VALUE;
import static org.totschnig.myexpenses.util.UiUtils.DateMode.DATE;
import static org.totschnig.myexpenses.util.UiUtils.DateMode.DATE_TIME;

@RunWith(RobolectricTestRunner.class)
public class UiUtilsDateModeTest {
  private Account cashAccount, bankAcount;
  private PrefHandler prefHandler;

  @Before
  public void setup() {
    cashAccount = Mockito.mock(Account.class);
    bankAcount = Mockito.mock(Account.class);
    Mockito.when(cashAccount.getType()).thenReturn(AccountType.CASH);
    Mockito.when(bankAcount.getType()).thenReturn(AccountType.BANK);
    prefHandler = Mockito.mock(PrefHandler.class);
  }

  private void mockPref(PrefKey prefKey, boolean value) {
    Mockito.when(prefHandler.getBoolean(eq(prefKey), anyBoolean())).thenReturn(value);
  }

  @Test
  public void withTimeAndWithValueDate() {
    mockPref(TRANSACTION_WITH_TIME, true);
    mockPref(TRANSACTION_WITH_VALUE_DATE, true);

    Assert.assertEquals(DATE_TIME, dateMode(cashAccount));
    Assert.assertEquals(BOOKING_VALUE, dateMode(bankAcount));
  }

  @Test
  public void withTimeAndWithoutValueDate() {
    mockPref(TRANSACTION_WITH_TIME, true);
    mockPref(TRANSACTION_WITH_VALUE_DATE, false);

    Assert.assertEquals(DATE_TIME, dateMode(cashAccount));
    Assert.assertEquals(DATE_TIME, dateMode(bankAcount));
  }

  @Test
  public void withoutTimeAndWithValueDate() {
    mockPref(TRANSACTION_WITH_TIME, false);
    mockPref(TRANSACTION_WITH_VALUE_DATE, true);

    Assert.assertEquals(DATE, dateMode(cashAccount));
    Assert.assertEquals(BOOKING_VALUE, dateMode(bankAcount));
  }

  @Test
  public void withoutTimeAndWithoutValueDate() {
    mockPref(TRANSACTION_WITH_TIME, false);
    mockPref(TRANSACTION_WITH_VALUE_DATE, false);

    Assert.assertEquals(DATE, dateMode(cashAccount));
    Assert.assertEquals(DATE, dateMode(bankAcount));
  }

  private UiUtils.DateMode dateMode(Account account) {
    return  UiUtils.getDateMode(account.getType(), prefHandler);
  }
}
