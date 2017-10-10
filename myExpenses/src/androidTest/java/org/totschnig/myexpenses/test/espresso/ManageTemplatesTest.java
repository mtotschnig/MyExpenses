package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.widget.AdapterView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;

import java.util.Currency;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.totschnig.myexpenses.testutils.Matchers.inToast;

//TODO test CAB actions
public class ManageTemplatesTest {

  @Rule
  public IntentsTestRule<ManageTemplates> mActivityRule =
      new IntentsTestRule<>(ManageTemplates.class, false, false);
  private static Template template;
  private static Account account;

  @Before
  public void fixture() {
    account = new Account("Test account 1", Currency.getInstance("EUR"), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    template = new Template(account, Transaction.TYPE_TRANSACTION, null);
    template.setAmount(new Money(account.currency, -1200L));
    template.setTitle("Espresso Test Template");
    template.save();
    assertThat(Transaction.countPerAccount(account.getId())).isEqualTo(0);
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ManageTemplates.class);
    mActivityRule.launchActivity(i);
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  private void verifyEditAction() {
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  private void verifySaveAction() {
    assertThat(Transaction.countPerAccount(account.getId())).isEqualTo(1);
    String success = mActivityRule.getActivity().getResources()
        .getQuantityString(R.plurals.save_transaction_from_template_success, 1, 1);
    onView(withText(success)).inRoot(inToast()).check(matches(isDisplayed()));
  }

  private void clicketiclick() {
    onData(anything()).inAdapterView(isAssignableFrom(AdapterView.class)).atPosition(0).perform(click());
  }

  @Test
  public void clickOnTemplateOpensDialogAndApplySaveActionIsTriggered() {
    clicketiclick();
    onView(withText(R.string.menu_create_instance_save)).perform(click());
    verifySaveAction();
  }

  @Test
  public void clickOnTemplateOpensDialogAndApplyEditActionIsTriggered() throws InterruptedException {
    clicketiclick();
    onView(withText(R.string.menu_create_instance_edit)).perform(click());
    verifyEditAction();
  }

  @Test
  public void clickOnTemplateWithDefaultActionApplySave() {
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.putBoolean(true);
    PrefKey.TEMPLATE_CLICK_DEFAULT.putString("SAVE");
    clicketiclick();
    verifySaveAction();
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.remove();
    PrefKey.TEMPLATE_CLICK_DEFAULT.remove();
  }

  @Test
  public void clickOnTemplateWithDefaultActionApplyEdit() {
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.putBoolean(true);
    PrefKey.TEMPLATE_CLICK_DEFAULT.putString("EDIT");
    clicketiclick();
    verifyEditAction();
    PrefKey.TEMPLATE_CLICK_HINT_SHOWN.remove();
    PrefKey.TEMPLATE_CLICK_DEFAULT.remove();
  }
}
