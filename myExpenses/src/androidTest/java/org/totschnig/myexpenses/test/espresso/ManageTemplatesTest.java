package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
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
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.util.Currency;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;

//TODO test CAB actions
public class ManageTemplatesTest extends BaseUiTest {

  @Rule
  public IntentsTestRule<ManageTemplates> mActivityRule =
      new IntentsTestRule<>(ManageTemplates.class, false, false);
  private static Template templateTransaction, templateTransfer, templateSplit;
  private static Account account1, account2;

  @Before
  public void fixture() {
    account1 = new Account("Test account 1", CurrencyUnit.create(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account("Test account 1", CurrencyUnit.create(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    templateTransaction = new Template(account1, TYPE_TRANSACTION, null);
    templateTransaction.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    templateTransaction.setTitle("Espresso Transaction Template");
    templateTransaction.save();
    templateTransfer = Template.getTypedNewInstance(TYPE_TRANSFER, account1.getId(), false, null);
    templateTransfer.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    templateTransfer.setTransferAccountId(account2.getId());
    templateTransfer.setTitle("Espresso Transfer Template");
    templateTransfer.save();
    templateSplit = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, null);
    templateSplit.setAmount(new Money(account1.getCurrencyUnit(), -1200L));
    templateSplit.setTitle("Espresso Split Template");
    templateSplit.save(true);
    Template part = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, templateSplit.getId());
    part.save();
    assertThat(Transaction.countPerAccount(account1.getId())).isEqualTo(0);
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ManageTemplates.class);
    mActivityRule.launchActivity(i);
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
  }

  private void verifyEditAction() {
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  private void verifySaveAction() {
    assertThat(Transaction. count(Transaction.CONTENT_URI, KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " IS NULL",
        new String[]{String.valueOf(account1.getId())})).isEqualTo(1);
  }

  @Test
  public void clickOnTemplateOpensDialogAndApplySaveActionIsTriggered() {
    clickOnFirstListEntry();
    onView(withText(R.string.menu_create_instance_save)).perform(click());
    verifySaveAction();
  }

  @Test
  public void clickOnTemplateOpensDialogAndApplyEditActionIsTriggered() {
    clickOnFirstListEntry();
    onView(withText(R.string.menu_create_instance_edit)).perform(click());
    verifyEditAction();
  }

  @Test
  public void defaultActionEditWithTransaction() {
    doTheTest("EDIT", "Espresso Transaction Template");
  }

  @Test
  public void defaultActionSavetWithTransaction() {
    doTheTest("SAVE", "Espresso Transaction Template");
  }

  @Test
  public void defaultActionEditWithTransfer() {
    doTheTest("EDIT", "Espresso Transfer Template");
  }

  @Test
  public void defaultActionSaveWithTransfer() {
    doTheTest("SAVE", "Espresso Transfer Template");
  }

  @Test
  public void defaultActionEditWithSplit() {
    unlock();
    doTheTest("EDIT", "Espresso Split Template");
  }

  @Test
  public void defaultActionSaveWithSplit() {
    unlock();
    doTheTest("SAVE", "Espresso Split Template");
  }


  private void doTheTest(String action, String title) {
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.putBoolean(true);
    PrefKey.TEMPLATE_CLICK_DEFAULT.putString(action);
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_TITLE, title))
        .perform(click());
    switch (action) {
      case "SAVE": verifySaveAction(); break;
      case "EDIT": verifyEditAction(); break;
    }
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.remove();
    PrefKey.TEMPLATE_CLICK_DEFAULT.remove();
  }

  private void unlock() {
    final AppComponent appComponent = ((MyApplication) mActivityRule.getActivity().getApplicationContext()).getAppComponent();
    LicenceHandler licenceHandler = appComponent.licenceHandler();
    licenceHandler.setLockState(false);
  }


  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
