/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.test.model;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CategoryCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER;

public class AccountTest extends ModelTest {
  public static final String TEST_CAT = "TestCat";
  Account account1, account2;
  Long openingBalance = 100L,
      expense1 = 10L,
      expense2 = 20L,
      income1 = 30L,
      income2 = 40L,
      transferP = 50L,
      transferN = 60L;
  private long catId;
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (account1 != null) {
      Account.delete(account1.getId());
    }
    if (account2 != null) {
      Account.delete(account2.getId());
    }
    Category.delete(catId);
  }

  private void insertData() {
    Transaction op;
    account1 = new Account("Account 1", openingBalance, "Account 1");
    account1.save();
    account2 = new Account("Account 2", openingBalance, "Account 2");
    account2.save();
    catId = Category.write(0, TEST_CAT, null);
    op = Transaction.getNewInstance(account1.getId());
    assert op != null;
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense1));
    op.setCrStatus(CrStatus.CLEARED);
    op.save();
    op.setAmount(new Money(account1.getCurrencyUnit(), -expense2));
    op.saveAsNew();
    op.setAmount(new Money(account1.getCurrencyUnit(), income1));
    op.saveAsNew();
    op.setAmount(new Money(account1.getCurrencyUnit(), income2));
    op.setCatId(catId);
    op.saveAsNew();
    Transfer op1 = Transfer.getNewInstance(account1.getId(), account2.getId());
    assert op1 != null;
    op1.setAmount(new Money(account1.getCurrencyUnit(), transferP));
    op1.save();
    op1.setAmount(new Money(account1.getCurrencyUnit(), -transferN));
    op1.saveAsNew();
  }


  public void testAccount() throws RemoteException, OperationApplicationException {
    Account account, restored;
    Long openingBalance = (long) 100;
    account = new Account("TestAccount", openingBalance, "Testing with Junit");
    account.setCurrency(new CurrencyUnit(java.util.Currency.getInstance("EUR")));
    assertEquals("EUR", account.getCurrencyUnit().getCode());
    account.save();
    assertTrue(account.getId() > 0);
    restored = Account.getInstanceFromDb(account.getId());
    assertEquals(account, restored);
    Long trAmount = (long) 100;
    Transaction op1 = Transaction.getNewInstance(account.getId());
    assert op1 != null;
    op1.setAmount(new Money(account.getCurrencyUnit(), trAmount));
    op1.setComment("test transaction");
    op1.save();
    assertEquals(account.getTotalBalance().getAmountMinor(), openingBalance + trAmount);
    Account.delete(account.getId());
    assertNull("Account deleted, but can still be retrieved", Account.getInstanceFromDb(account.getId()));
    assertNull("Account delete should delete transaction, but operation can still be retrieved", Transaction.getInstanceFromDb(op1.getId()));
  }

  /**
   * we test if the db calculates the aggregate sums correctly
   * this is rather a test of the cursor exposed through the content provider
   * but set up is easier through models
   */
  public void testDatabaseCalculatedSums() {
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        null,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    //the database setup creates the default account
    assert cursor != null;
    insertData();
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        Account.PROJECTION_FULL,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assert cursor != null;
    assertEquals(2, cursor.getCount());
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        Account.PROJECTION_FULL,            // get all the columns
        KEY_ROWID + "=" + account1.getId(),
        null,                       // no selection criteria
        null                        // use default the sort order
    );
    assert cursor != null;
    assertTrue(cursor.moveToFirst());

    // Since no projection was used, get the column indexes of the returned columns
    int incomeIndex = cursor.getColumnIndex(KEY_SUM_INCOME);
    int expensesIndex = cursor.getColumnIndex(KEY_SUM_EXPENSES);
    int transferIndex = cursor.getColumnIndex(KEY_SUM_TRANSFERS);
    int balanceIndex = cursor.getColumnIndex(KEY_CURRENT_BALANCE);
    assertEquals(income1 + income2, cursor.getLong(incomeIndex));
    assertEquals(-expense1 - expense2, cursor.getLong(expensesIndex));
    assertEquals(transferP - transferN, cursor.getLong(transferIndex));
    assertEquals(openingBalance + income1 + income2 - expense1 - expense2 + transferP - transferN, cursor.getLong(balanceIndex));
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        Account.PROJECTION_FULL,            // get all the columns
        KEY_ROWID + "=" + account2.getId(),
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assert cursor != null;
    assertTrue(cursor.moveToFirst());
    assertEquals(0L, cursor.getLong(incomeIndex));
    assertEquals(0L, cursor.getLong(expensesIndex));
    assertEquals(transferN - transferP, cursor.getLong(transferIndex));
    assertEquals(openingBalance + transferN - transferP, cursor.getLong(balanceIndex));
    cursor.close();
  }

  public void testGetInstanceZeroReturnsAccount() {
    //without inserting, there is no account in the database
    assertNull("getInstanceFromDb(0) should return null on an empty database", Account.getInstanceFromDb(0));
  }

  public void testGetAggregateAccountFromDb() {
    insertData();
    String currency = Utils.getHomeCurrency().getCode();
    Cursor c = getMockContentResolver().query(
        TransactionProvider.CURRENCIES_URI,
        new String[]{KEY_ROWID},
        KEY_CODE + " = ?",
        new String[]{currency},
        null);
    assert c != null;
    c.moveToFirst();
    long id = 0 - c.getLong(0);
    c.close();
    AggregateAccount aa = (AggregateAccount) Account.getInstanceFromDb(id);
    assert aa != null;
    assertEquals(currency, aa.getCurrencyUnit().getCode());
    assertEquals(openingBalance * 2, aa.openingBalance.getAmountMinor());
  }

  public void testBalanceWithoutReset() {
    insertData();
    Money initialclearedBalance = account1.getClearedBalance();
    assertFalse(initialclearedBalance.equals(account1.getReconciledBalance()));
    assertEquals(4, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(0, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    account1.balance(false);
    assertEquals(0, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(4, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    assertEquals(initialclearedBalance, account1.getReconciledBalance());
  }

  public void testBalanceWithReset() {
    insertData();
    Money initialclearedBalance = account1.getClearedBalance();
    assertFalse(initialclearedBalance.equals(account1.getReconciledBalance()));
    assertEquals(4, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(0, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    account1.balance(true);
    assertEquals(0, count(account1.getId(), KEY_CR_STATUS + " != '" + CrStatus.UNRECONCILED.name() + "'"));
    assertEquals(2, count(account1.getId(), KEY_CR_STATUS + " = '" + CrStatus.UNRECONCILED.name() + "'"));
    assertEquals(initialclearedBalance, account1.getReconciledBalance());

  }

  public void testReset() throws OperationApplicationException, RemoteException {
    insertData();
    Money initialtotalBalance = account1.getTotalBalance();
    assertEquals(6, count(account1.getId(), null));
    account1.reset(null, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    assertEquals(0, count(account1.getId(), null));
    Account resetAccount = Account.getInstanceFromDb(account1.getId());
    assert resetAccount != null;
    assertEquals(initialtotalBalance, resetAccount.getTotalBalance());
  }

  public void testResetWithFilterUpdateBalance() throws OperationApplicationException, RemoteException {
    insertData();
    Money initialtotalBalance = account1.getTotalBalance();
    assertEquals(6, count(account1.getId(), null));
    WhereFilter filter = WhereFilter.empty();
    filter.put(new CategoryCriteria(TEST_CAT, catId));
    account1.reset(filter, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    assertEquals(5, count(account1.getId(), null));//1 Transaction deleted
    Account resetAccount = Account.getInstanceFromDb(account1.getId());
    assert resetAccount != null;
    assertEquals(initialtotalBalance, resetAccount.getTotalBalance());
  }

  public void testResetWithFilterCreateHelper() throws OperationApplicationException, RemoteException {
    insertData();
    Money initialtotalBalance = account1.getTotalBalance();
    assertEquals(6, count(account1.getId(), null));
    assertEquals(1, count(account1.getId(), KEY_CATID + "=" + catId));
    assertEquals(0, count(account1.getId(), KEY_STATUS + "=" + STATUS_HELPER));
    WhereFilter filter = WhereFilter.empty();
    filter.put(new CategoryCriteria(TEST_CAT, catId));
    account1.reset(filter, Account.EXPORT_HANDLE_DELETED_CREATE_HELPER, null);
    assertEquals(6, count(account1.getId(), null));//-1 Transaction deleted;+1 helper
    assertEquals(0, count(account1.getId(), KEY_CATID + "=" + catId));
    assertEquals(1, count(account1.getId(), KEY_STATUS + "=" + STATUS_HELPER));
    Account resetAccount = Account.getInstanceFromDb(account1.getId());
    assert resetAccount != null;
    assertEquals(initialtotalBalance, resetAccount.getTotalBalance());
  }

  /**
   * @param accountId id of account to be counted
   * @param condition if not null interpreted as a where clause for filtering the transactions
   * @return the number of transactions in an account
   */
  private int count(long accountId, String condition) {
    int result = 0;
    String selection = KEY_ACCOUNTID + " = ?";
    if (condition != null) {
      selection += " AND " + condition;
    }
    Cursor c = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,
        new String[]{"count(*)"},
        selection,
        new String[]{String.valueOf(accountId)},
        null
    );
    if (c == null) {
      return result;
    }
    if (c.moveToFirst()) result = c.getInt(0);
    c.close();
    return result;
  }
}
