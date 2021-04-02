package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import java.util.Currency;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;


public class ForeignTransferEditTest extends BaseUiTest {

  private ActivityScenario<ExpenseEdit> activityScenario = null;
  private Account account1;
  private Account account2;
  private Transfer transfer;

  @Before
  public void fixture() {
    CurrencyUnit currency1 = new CurrencyUnit(Currency.getInstance("USD"));
    CurrencyUnit currency2 = new CurrencyUnit(Currency.getInstance("EUR"));
    String accountLabel1 = "Test label 1";
    account1 = new Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    String accountLabel2 = "Test label 2";
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
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    activityScenario = ActivityScenario.launch(i);
    closeKeyboardAndSave();
    assertFinishing();
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
