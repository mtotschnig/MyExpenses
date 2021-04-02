package org.totschnig.myexpenses.test.espresso;


import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.ui.AmountInput;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.ACCOUNT_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.AMOUNT_MICROS;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.CATEGORY_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.COMMENT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.PAYEE_NAME;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible;
import static org.totschnig.myexpenses.testutils.Espresso.withIdAndAncestor;
import static org.totschnig.myexpenses.testutils.Espresso.withIdAndParent;
import static org.totschnig.myexpenses.testutils.Matchers.withListSize;
import static org.totschnig.myexpenses.testutils.MoreMatchersKt.toolbarTitle;

public class ExpenseEditLoadDataTest extends BaseUiTest {
  private static CurrencyUnit currency, foreignCurrency;
  private ActivityScenario<ExpenseEdit> activityScenario = null;
  @Rule
  public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR);
  private Account account1;
  private Account account2;
  private Transaction transaction;
  private Transfer transfer;

  @Before
  public void fixture() {
    //IdlingRegistry.getInstance().register(getIdlingResource());
    currency = new CurrencyUnit(Currency.getInstance("EUR"));
    foreignCurrency = new CurrencyUnit(Currency.getInstance("USD"));
    account1 = new Account("Test account 1", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account("Test account 2", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    transaction = Transaction.getNewInstance(account1.getId());
    transaction.setAmount(new Money(currency, 500L));
    transaction.save();
    transfer = Transfer.getNewInstance(account1.getId(), account2.getId());
    transfer.setAmount(new Money(currency, 600L));
    transfer.save();
  }

/*
 use of IdlingThreadPoolExecutor unfortunately does not prevent the tests from failing
 it seems that espresso after execution of coroutine immediately thinks app is idle, before UI has
 been populated with data
 at the moment we load data on main thread in test
  private IdlingThreadPoolExecutor getIdlingResource() {
    return ((TestApp) InstrumentationRegistry.getTargetContext().getApplicationContext()).getTestCoroutineModule().getExecutor();
  }*/

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
    activityScenario.close();
    //IdlingRegistry.getInstance().unregister(getIdlingResource());
  }

  @Test
  public void shouldPopulateWithTransactionAndPrepareForm() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transaction.getId());
    launchAndWait(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("5")));
  }

  @Test
  public void shouldKeepStatusAndUuidAfterSave() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transaction.getId());
    String uuid = transaction.getUuid();
    int status = transaction.getStatus();
    launchAndWait(i);
    closeKeyboardAndSave();
    Transaction t = Transaction.getInstanceFromDb(transaction.getId());
    assertThat(t.getStatus()).isEqualTo(status);
    assertThat(t.getUuid()).isEqualTo(uuid);
  }

  @Test
  public void shouldPopulateWithForeignExchangeTransfer() throws Exception {
    Account foreignAccount = new Account("Test account 2", foreignCurrency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    foreignAccount.save();
    Transfer foreignTransfer = Transfer.getNewInstance(account1.getId(), foreignAccount.getId());
    foreignTransfer.setAmountAndTransferAmount(new Money(currency, 100L), new Money(foreignCurrency, 200L));
    foreignTransfer.save();
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, foreignTransfer.getId());
    launchAndWait(i);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("1")));
    onView(withIdAndParent(R.id.AmountEditText, R.id.TransferAmount)).check(matches(withText("2")));
    onView(withIdAndAncestor(R.id.ExchangeRateEdit1, R.id.ExchangeRate)).check(matches(withText("2")));
    onView(withIdAndAncestor(R.id.ExchangeRateEdit2, R.id.ExchangeRate)).check(matches(withText(formatAmount(0.5f))));
    Account.delete(foreignAccount.getId());
  }

  private String formatAmount(float amount) {
    return new DecimalFormat("0.##").format(amount);
  }

  private void launchAndWait(Intent i) {
    activityScenario = ActivityScenario.launch(i);
  }

  @Test
  public void shouldPopulateWithTransferAndPrepareForm() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    launchAndWait(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
        R.id.TransferAccountRow);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldSwitchAccountViewsForReceivingTransferPart() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    launchAndWait(i);
    activityScenario.onActivity(activity -> {
      assertThat(((AmountInput) activity.findViewById(R.id.Amount)).getType()).isTrue();
      assertThat(((ViewGroup) activity.findViewById(R.id.AccountRow)).getChildAt(1).getId()).isEqualTo(R.id.TransferAccount);
    });
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldKeepAccountViewsForGivingTransferPart() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getTransferPeer());
    launchAndWait(i);
    activityScenario.onActivity(activity -> {
      assertThat(((AmountInput) activity.findViewById(R.id.Amount)).getType()).isFalse();
      assertThat(((ViewGroup) activity.findViewById(R.id.AccountRow)).getChildAt(1).getId()).isEqualTo(R.id.Account);
    });
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldPopulateWithSplitTransactionAndPrepareForm() {
    Transaction splitTransaction = SplitTransaction.getNewInstance(account1.getId());
    splitTransaction.setStatus(DatabaseConstants.STATUS_NONE);
    splitTransaction.save(true);
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, splitTransaction.getId());
    launchAndWait(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow);
  }

  @Test
  public void shouldPopulateWithSplitTemplateAndLoadParts() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, buildSplitTemplate());
    launchAndWait(i);
    activityScenario.onActivity(activity -> assertThat(activity.isTemplate).isTrue());
    toolbarTitle().check(matches(withSubstring(getString(R.string.menu_edit_template))));
    checkEffectiveVisible(R.id.SplitContainer);
    onView(withId(R.id.list)).check(matches(withListSize(1)));
  }

  @Test
  public void shouldPopulateFromSplitTemplateAndLoadParts() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, buildSplitTemplate());
    i.putExtra(KEY_INSTANCEID, -1L);
    launchAndWait(i);
    activityScenario.onActivity(activity -> assertThat(activity.isTemplate).isFalse());
    onView(withId(R.id.OperationType)).check(matches(withSpinnerText(R.string.menu_create_split)));
    checkEffectiveVisible(R.id.SplitContainer);
    onView(withId(R.id.list)).check(matches(withListSize(1)));
  }

  private long buildSplitTemplate() {
    Template template = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, null);
    template.save(true);
    Template part = Template.getTypedNewInstance(TYPE_SPLIT, account1.getId(), false, template.getId());
    part.save();
    return template.getId();
  }

  @Test
  public void shouldPopulateWithPlanAndPrepareForm() {
    Template plan = Template.getTypedNewInstance(TYPE_TRANSACTION, account1.getId(), false, null);
    plan.setTitle("Daily plan");
    plan.setAmount(new Money(currency, 700L));
    plan.setPlan(new Plan(LocalDate.now(), Plan.Recurrence.DAILY, "Daily", plan.compileDescription(getApp())));
    plan.save();
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, plan.getId());
    launchAndWait(i);
    checkEffectiveVisible(R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.PB);
    checkEffectiveGone(R.id.Recurrence);
    activityScenario.onActivity(activity -> assertThat(activity.isTemplate).isTrue());
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("7")));
    onView(withId(R.id.Title)).check(matches(withText("Daily plan")));
  }

  @Test
  public void shouldInstantiateFromTemplateAndPrepareForm() {
    Template template = Template.getTypedNewInstance(TYPE_TRANSACTION, account1.getId(), false, null);
    template.setTitle("Nothing but a plan");
    template.setAmount(new Money(currency, 800L));
    template.save();
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, template.getId());
    i.putExtra(KEY_INSTANCEID, -1L);
    launchAndWait(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow);
    checkEffectiveGone(R.id.PB, R.id.TitleRow);
    activityScenario.onActivity(activity -> assertThat(activity.isTemplate).isFalse());
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("8")));
  }

  @Test
  public void shouldPopulateFromIntent() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.setAction(Intent.ACTION_INSERT);
    i.putExtra(ACCOUNT_LABEL, account1.getLabel());
    i.putExtra(AMOUNT_MICROS, 1230000L);
    i.putExtra(PAYEE_NAME, "John Doe");
    i.putExtra(CATEGORY_LABEL, "A");
    i.putExtra(COMMENT, "A note");
    launchAndWait(i);
    onView(withId(R.id.Account)).check(matches(withSpinnerText(account1.getLabel())));
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText(formatAmount(1.23F))));
    onView(withId(R.id.Payee)).check(matches(withText("John Doe")));
    onView(withId(R.id.Comment)).check(matches(withText("A note")));
    onView(withId(R.id.Category)).check(matches(withText("A")));
    Category.delete(Category.find("A", null));
  }

  @Test
  public void shouldNotEditSealed() throws Exception {
    Account sealedAccount = new Account("Sealed account", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    sealedAccount.save();
    Transaction sealed = Transaction.getNewInstance(sealedAccount.getId());
    sealed.setAmount(new Money(currency, 500L));
    sealed.save();
    ContentValues values = new ContentValues(1);
    values.put(KEY_SEALED, true);
    getApp().getContentResolver().update(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, sealedAccount.getId()), values, null, null);
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, sealed.getId());
    activityScenario = ActivityScenario.launch(i);
    assertCanceled();
    Account.delete(sealedAccount.getId());
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
