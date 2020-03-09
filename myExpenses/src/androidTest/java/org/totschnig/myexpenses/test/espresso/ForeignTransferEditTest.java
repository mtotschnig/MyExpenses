package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transfer;

import java.util.Currency;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class ForeignTransferEditTest {

  @Rule
  public ActivityTestRule<ExpenseEdit> mActivityRule =
      new ActivityTestRule<>(ExpenseEdit.class, false, false);
  private String accountLabel1 = "Test label 1";
  private String accountLabel2 = "Test label 2";
  private Account account1;
  private Account account2;
  private CurrencyUnit currency1;
  private CurrencyUnit currency2;
  private Transfer transfer;

  @Before
  public void fixture() {
    currency1 = CurrencyUnit.create(Currency.getInstance("USD"));
    currency2 = CurrencyUnit.create(Currency.getInstance("EUR"));
    account1 = new Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account(accountLabel2, currency2, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    transfer = Transfer.getNewInstance(account1.getId(), account2.getId());
    transfer.setAmountAndTransferAmount(new Money(currency1, -2000L), new Money(currency2, -3000L));
    transfer.save();
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void shouldSaveForeignTransfer() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    mActivityRule.launchActivity(i);
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    assertTrue(mActivityRule.getActivity().isFinishing());
  }
}
