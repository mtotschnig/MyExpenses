package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.util.Currency;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.CursorMatchers;

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

//TODO test CAB actions
public class ManageTemplatesTest extends BaseUiTest {

  private ActivityScenario<ManageTemplates> activityScenario = null;

  private static Account account1, account2;

  @Before
  public void fixture() {
    account1 = new Account("Test account 1", new CurrencyUnit(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account("Test account 1", new CurrencyUnit(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    createInstances(Template.Action.SAVE);
    createInstances(Template.Action.EDIT);
    Intent i = new Intent(getTargetContext(), ManageTemplates.class);
    activityScenario = ActivityScenario.launch(i);
    Intents.init();
  }

  public void createInstances(Template.Action defaultAction) {
    Template template = new Template(account1, TYPE_TRANSACTION, null);
    template.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    template.setDefaultAction(defaultAction);
    template.setTitle("Espresso Transaction Template " + defaultAction.name());
    template.save();
    template = Template.getTypedNewInstance(TYPE_TRANSFER, account1.getId(), false, null);
    template.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    template.setTransferAccountId(account2.getId());
    template.setTitle("Espresso Transfer Template " + defaultAction.name());
    template.setDefaultAction(defaultAction);
    template.save();
    template = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, null);
    template.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    template.setTitle("Espresso Split Template " + defaultAction.name());
    template.setDefaultAction(defaultAction);
    template.save(true);
    Template part = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, template.getId());
    part.save();
    assertThat(Transaction.countPerAccount(account1.getId())).isEqualTo(0);
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Intents.release();
  }

  private void verifyEditAction() {
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  private void verifySaveAction() {
    assertThat(Transaction. count(Transaction.CONTENT_URI, KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " IS NULL",
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
      case "SAVE": verifySaveAction(); break;
      case "EDIT": verifyEditAction(); break;
    }
  }

  private void unlock() {
    final AppComponent appComponent = getApp().getAppComponent();
    LicenceHandler licenceHandler = appComponent.licenceHandler();
    licenceHandler.setLockState(false);
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
