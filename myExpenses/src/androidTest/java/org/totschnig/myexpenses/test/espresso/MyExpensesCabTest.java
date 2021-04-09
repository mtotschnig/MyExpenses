package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import java.util.Currency;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.filters.FlakyTest;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.testutils.Matchers.withAdaptedData;

public final class MyExpensesCabTest extends BaseUiTest {

  private ActivityScenario<MyExpenses> activityScenario = null;
  private Account account;

  @Before
  public void fixture() {
    account = new Account("Test account 1", new CurrencyUnit(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    Transaction op0 = Transaction.getNewInstance(account.getId());
    op0.setAmount(new Money(new CurrencyUnit(Currency.getInstance("USD")), -1200L));
    op0.save();
    int times = 5;
    for (int i = 0; i < times; i++) {
      op0.saveAsNew();
    }
    Intent i = new Intent(getTargetContext(), MyExpenses.class);
    i.putExtra(KEY_ROWID, account.getId());
    activityScenario = ActivityScenario.launch(i);
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Test
  @FlakyTest
  public void cloneCommandIncreasesListSize() throws TimeoutException {
    int origListSize = waitForAdapter().getCount();
    openCab();
    clickMenuItem(R.id.CLONE_TRANSACTION_COMMAND, true);
    closeKeyboardAndSave();
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize + 1);
  }

  @Test
  @FlakyTest
  public void editCommandKeepsListSize() throws TimeoutException {
    int origListSize = waitForAdapter().getCount();
    openCab();
    clickMenuItem(R.id.EDIT_COMMAND, true);
    closeKeyboardAndSave();
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize);
    }

  @Test
  public void createTemplateCommandCreatesTemplate() throws TimeoutException {
    waitForAdapter();
    String templateTitle = "Espresso Template Test";
    openCab();
    clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND, true);
    onView(withText(containsString(getString(R.string.menu_create_template))))
        .check(matches(isDisplayed()));
    onView(withId(R.id.editText))
        .perform(typeText(templateTitle));
    closeSoftKeyboard();
    onView(withText(R.string.dialog_button_add)).perform(click());
    onView(withId(R.id.CREATE_COMMAND)).perform(click());

    //((EditText) mSolo.getView(EditText.class, 0)).onEditorAction(EditorInfo.IME_ACTION_DONE);
    clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND);
    onView(withText(is(templateTitle))).check(matches(isDisplayed()));
  }

  @Test
  public void deleteCommandDecreasesListSize() throws TimeoutException {
    int origListSize = waitForAdapter().getCount();
    openCab();
    clickMenuItem(R.id.DELETE_COMMAND, true);
    onView(withText(R.string.menu_delete)).perform(click());
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize - 1);
  }

  @Test
  public void deleteCommandWithVoidOption() throws TimeoutException {
    int origListSize = waitForAdapter().getCount();
    openCab();
    clickMenuItem(R.id.DELETE_COMMAND, true);
    onView(withId(R.id.checkBox)).perform(click());
    onView(withText(R.string.menu_delete)).perform(click());
    onData(is(instanceOf(Cursor.class))).inAdapterView(getWrappedList()).atPosition(1)
        .check(matches(hasDescendant(both(withId(R.id.voidMarker)).and(isDisplayed()))));
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize);
    openCab();
    clickMenuItem(R.id.UNDELETE_COMMAND, true);
    onView(getWrappedList())
        .check(matches(not(withAdaptedData(CursorMatchers.withRowString(DatabaseConstants.KEY_CR_STATUS, "VOID")))));
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize);
  }

  @Test
  public void deleteCommandCancelKeepsListSize() throws TimeoutException {
    int origListSize = waitForAdapter().getCount();
    openCab();
    clickMenuItem(R.id.DELETE_COMMAND, true);
    onView(withText(android.R.string.cancel)).perform(click());
    assertThat(waitForAdapter().getCount()).isEqualTo(origListSize);
  }

  @Test
  public void splitCommandCreatesSplitTransaction() throws TimeoutException {
    waitForAdapter();
    openCab();
    clickMenuItem(R.id.SPLIT_TRANSACTION_COMMAND, true);
    handleContribDialog(ContribFeature.SPLIT_TRANSACTION);
    onView(withText(R.string.menu_split_transaction)).perform(click());
    onView(withText(R.string.split_transaction)).check(matches(isDisplayed()));
    //CursorMatchers class does not allow to distinguish between null and 0 in database
/*    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_CATID, DatabaseConstants.SPLIT_CATID))
        .inAdapterView(allOf(
            isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.list)),
            isDisplayed())).check(matches(isDisplayed()));*/
  }

  @Test
  public void cabIsRestoredAfterOrientationChange() throws TimeoutException {
    waitForAdapter();
    openCab();
    rotate();
    onView(withId(R.id.action_mode_bar)).check(matches(isDisplayed()));
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
