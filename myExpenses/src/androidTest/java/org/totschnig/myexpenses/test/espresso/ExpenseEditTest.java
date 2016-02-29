package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.CursorMatchers;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.widget.Spinner;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.Currency;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertEquals;

/**
 * Created by michaeltotschnig on 29.02.16.
 */
public class ExpenseEditTest {

  @Rule
  public ActivityTestRule<ExpenseEdit> mActivityRule =
      new ActivityTestRule<>(ExpenseEdit.class, false, false);
  private static String accountLabel1 = "Test label 1";
  private static String accountLabel2 = "Test label 2";
  private static Account account1;
  private static Account account2;
  private static Currency currency1 = Currency.getInstance("USD");
  private static Currency currency2 = Currency.getInstance("EUR");

  @BeforeClass
  public static void fixture() {
    account1 = new Account(accountLabel1, currency1, 0, "", Account.Type.CASH, Account.defaultColor);
    account1.save();
    account2 = new Account(accountLabel2, currency2, 0, "", Account.Type.CASH, Account.defaultColor);
    account2.save();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void formForTransactionIsPrepared() {
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
    mActivityRule.launchActivity(i);
    for (int resId : new int[]{
        R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow}
        ) {
      onView(withId(resId)).check(matches(isDisplayed()));
    }
  }

  @Test
  public void formForTransferIsPrepared() {
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSFER);
    mActivityRule.launchActivity(i);
    for (int resId : new int[]{
        R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow, R.id.TransferAccountRow}
        ) {
      onView(withId(resId)).check(matches(isDisplayed()));
    }
  }

  @Test
  public void formForSplitIsPrepared() {
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_SPLIT);
    mActivityRule.launchActivity(i);
    for (int resId : new int[]{
        R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow}
        ) {
      onView(withId(resId)).check(matches(isDisplayed()));
    }
  }

  @Test
  public void accountIdInExtraShouldPopulateSpinner() {
    Account[] allAccounts = {account1, account2};
    for (Account a: allAccounts) {
      Intent i = new Intent(Intent.ACTION_EDIT);
      i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
      i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_ACCOUNTID, a.getId());
      mActivityRule.launchActivity(i);
      onView(withId(R.id.Account)).check(matches(withSpinnerText(a.label)));
      mActivityRule.getActivity().finish();
    }
  }

  @Test
  public void currencyInExtraShouldPopulateSpinner() {
    Currency[] allCurrencies = {currency1, currency2};
    for (Currency c: allCurrencies) {
      //we assume that Fixture has set up the default account with id 1
      Intent i = new Intent(Intent.ACTION_EDIT);
      i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
      i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_CURRENCY, c.getCurrencyCode());
      mActivityRule.launchActivity(i);
      assertEquals("Account is not selected", c, mActivityRule.getActivity().getCurrentAccount().currency);
      mActivityRule.getActivity().finish();
    }
  }

  public static Matcher<View> withSpinnerText(final String string) {
    checkNotNull(string);
    final CursorMatchers.CursorMatcher cursorMatcher =
        CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL,string);
    return new BoundedMatcher<View, Spinner>(Spinner.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("with text: " + string);
        cursorMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(Spinner spinner) {
        return cursorMatcher.matchesSafely(((Cursor) spinner.getSelectedItem()));
      }
    };
  }
}
