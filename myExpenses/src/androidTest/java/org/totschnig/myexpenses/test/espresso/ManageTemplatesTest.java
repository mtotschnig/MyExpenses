package org.totschnig.myexpenses.test.espresso;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.CursorMatchers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model2.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;

//TODO test CAB actions
public class ManageTemplatesTest extends BaseUiTest<ManageTemplates> {
  private static Account account1, account2;

  @Before
  public void fixture() {
    account1 = buildAccount("Test account 1", 0);
    account2 = buildAccount("Test account 2", 0);
    createInstances(Template.Action.SAVE);
    createInstances(Template.Action.EDIT);
    Intent i = new Intent(getTargetContext(), ManageTemplates.class);
    testScenario = ActivityScenario.launch(i);
    Intents.init();
  }

  public void createInstances(Template.Action defaultAction) {
    CurrencyUnit currencyUnit = getHomeCurrency();
    Template template = new Template(getContentResolver(), account1.getId(), currencyUnit, TYPE_TRANSACTION, null);
    template.setAmount(new Money(currencyUnit, -1200L));
    template.setDefaultAction(defaultAction);
    template.setTitle("Espresso Transaction Template " + defaultAction.name());
    template.save(getContentResolver());
    template = Template.getTypedNewInstance(getContentResolver(), TYPE_TRANSFER, account1.getId(), currencyUnit, false, null);
    template.setAmount(new Money(currencyUnit, -1200L));
    template.setTransferAccountId(account2.getId());
    template.setTitle("Espresso Transfer Template " + defaultAction.name());
    template.setDefaultAction(defaultAction);
    template.save(getContentResolver());
    template = Template.getTypedNewInstance(getContentResolver(), TYPE_SPLIT, account1.getId(), currencyUnit, false, null);
    template.setAmount(new Money(currencyUnit, -1200L));
    template.setTitle("Espresso Split Template " + defaultAction.name());
    template.setDefaultAction(defaultAction);
    template.save(getContentResolver(), true);
    Template part = Template.getTypedNewInstance(getContentResolver(), TYPE_SPLIT, account1.getId(), currencyUnit, false, template.getId());
    part.save(getContentResolver());
    assertThat(getRepository().countTransactionsPerAccount(account1.getId())).isEqualTo(0);
  }

  @After
  public void tearDown() {
    Intents.release();
  }

  private void verifyEditAction() {
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  private void verifySaveAction() {
    assertThat(getRepository().count(Transaction.CONTENT_URI, KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " IS NULL",
        new String[]{String.valueOf(account1.getId())})).isEqualTo(1);
  }

  @Test
  public void defaultActionEditWithTransaction() {
    doTheTest("EDIT", "Transaction");
  }

  @Test
  public void defaultActionSaveWithTransaction() {
    doTheTest("SAVE", "Transaction");
  }

  @Test
  public void defaultActionEditWithTransfer() {
    doTheTest("EDIT", "Transfer");
  }

  @Test
  public void defaultActionSaveWithTransfer() {
    doTheTest("SAVE", "Transfer");
  }

  @Test
  public void defaultActionEditWithSplit() {
    unlock();
    doTheTest("EDIT", "Split");
  }

  @Test
  public void defaultActionSaveWithSplit() {
    unlock();
    doTheTest("SAVE", "Split");
  }

  private void doTheTest(String action, String type) {
    String title = String.format("Espresso %s Template %s", type, action);
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_TITLE, title))
        .perform(click());
    switch (action) {
      case "SAVE" -> verifySaveAction();
      case "EDIT" -> verifyEditAction();
    }
  }
}
