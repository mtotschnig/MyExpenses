package org.totschnig.myexpenses.provider;

import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;

import java.util.Currency;
import java.util.concurrent.TimeUnit;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.ACCOUNT_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.AMOUNT_MICROS;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.CATEGORY_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.COMMENT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.CURRENCY;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.DATE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.METHOD_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.PAYEE_NAME;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.REFERENCE_NUMBER;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TRANSFER_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;

@RunWith(RobolectricTestRunner.class)
public class ProviderUtilsTest {
  private Account euroAccount;
  private Account dollarAccount;

  @Before
  public void setupAccounts() {

    euroAccount = new Account("EUR-Account", new CurrencyUnit(Currency.getInstance("EUR")), 0L, null, AccountType.CASH, Account.DEFAULT_COLOR);
    euroAccount.save();
    dollarAccount = new Account("USD-Account", new CurrencyUnit(Currency.getInstance("USD")), 0L, null, AccountType.CASH, Account.DEFAULT_COLOR);
    dollarAccount.save();
  }

  @After
  public void deleteAccounts() throws RemoteException, OperationApplicationException {
    Account.delete(euroAccount.getId());
    Account.delete(dollarAccount.getId());
  }

  @Test
  public void shouldPickAccountBasedOnCurrency() {
    Bundle extras = new Bundle();
    extras.putString(CURRENCY, "EUR");
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(euroAccount.getId(), transaction.getAccountId());
    extras = new Bundle();
    extras.putString(CURRENCY, "USD");
    transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(dollarAccount.getId(), transaction.getAccountId());
  }

  @Test
  public void shouldPickAccountBasedOnLabel()  {
    Bundle extras = new Bundle();
    extras.putString(ACCOUNT_LABEL, "EUR-Account");
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(euroAccount.getId(), transaction.getAccountId());
    extras = new Bundle();
    extras.putString(ACCOUNT_LABEL, "USD-Account");
    transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(dollarAccount.getId(), transaction.getAccountId());
  }

  @Test
  public void shouldSetAmount() {
    Bundle extras = new Bundle();
    extras.putString(CURRENCY, "EUR");
    extras.putLong(AMOUNT_MICROS, 1230000);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(new Money(new CurrencyUnit(Currency.getInstance("EUR")), 123L), transaction.getAmount());
  }

  @Test
  public void shouldSetDate() {
    Bundle extras = new Bundle();
    long date = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)) / 1000;
    extras.putLong(DATE, date);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(date, transaction.getDate());
  }

  @Test
  public void shouldSetPayee() {
    Bundle extras = new Bundle();
    String payee = "John Doe";
    extras.putString(PAYEE_NAME, payee);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(payee, transaction.getPayee());
  }

  @Test
  public void shouldSetMainCategory() {
    Bundle extras = new Bundle();
    String category = "A";
    extras.putString(CATEGORY_LABEL, category);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(Category.find(category, null), transaction.getCatId().longValue());
    Assert.assertEquals(category, transaction.getLabel());
  }

  @Test
  public void shouldSetSubCategory() {
    Bundle extras = new Bundle();
    String category = "B:C";
    extras.putString(CATEGORY_LABEL, category);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(Category.find("C", Category.find("B", null)), transaction.getCatId().longValue());
    Assert.assertEquals(category, transaction.getLabel());
  }

  @Test
  public void shouldSetComment() {
    Bundle extras = new Bundle();
    String comment = "A note";
    extras.putString(COMMENT, comment);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(comment, transaction.getComment());
  }

  @Test
  public void shouldSetMethod() {
    Bundle extras = new Bundle();
    String method = "CHEQUE";
    extras.putString(METHOD_LABEL, method);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(PaymentMethod.find(method), transaction.getMethodId().longValue());
  }

  @Test
  public void shouldSetReferenceNumber() {
    Bundle extras = new Bundle();
    String number = "1";
    extras.putString(REFERENCE_NUMBER, number);
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertEquals(number, transaction.getReferenceNumber());
  }
  @Test
  public void shouldBuildTransfer() {
    Bundle extras = new Bundle();
    extras.putInt(OPERATION_TYPE, TYPE_TRANSFER);
    extras.putString(ACCOUNT_LABEL, "EUR-Account");
    extras.putString(TRANSFER_ACCOUNT_LABEL, "USD-Account");
    Transaction transaction = ProviderUtils.buildFromExtras(extras);
    Assert.assertTrue(transaction instanceof Transfer);
    Assert.assertEquals(dollarAccount.getId(), transaction.getTransferAccountId().longValue());
    Assert.assertEquals("USD-Account", transaction.getLabel());
  }
}