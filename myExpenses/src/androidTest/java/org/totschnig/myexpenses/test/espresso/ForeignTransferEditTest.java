package org.totschnig.myexpenses.test.espresso;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.model2.Account;

import java.util.Currency;


public class ForeignTransferEditTest extends BaseExpenseEditTest {

  private Transfer transfer;

  @Before
  public void fixture() {
    CurrencyUnit currency1 = new CurrencyUnit(Currency.getInstance("USD"));
    CurrencyUnit currency2 = new CurrencyUnit(Currency.getInstance("EUR"));
    String accountLabel1 = "Test label 1";
    Account account1 = buildAccount(accountLabel1, 0L, currency1.getCode());
    String accountLabel2 = "Test label 2";
    Account account2 = buildAccount(accountLabel2, 0L, currency1.getCode());
    transfer = Transfer.getNewInstance(account1.getId(), currency1, account2.getId());
    transfer.setAmountAndTransferAmount(new Money(currency1, -2000L), new Money(currency2, -3000L));
    transfer.save();
  }

  @Test
  public void shouldSaveForeignTransfer() {
    Intent i = getIntent();
    i.putExtra(KEY_ROWID, transfer.getId());
    testScenario = ActivityScenario.launchActivityForResult(i);
    closeKeyboardAndSave();
    assertFinishing();
  }
}
