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

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.model.Transaction.CrStatus;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;

public class AccountTest extends ModelTest  {
  Account account1, account2;
  Long openingBalance = 100L,
      expense1 = 10L,
      expense2 = 20L,
      income1 = 30L,
      income2 = 40L,
      transferP = 50L,
      transferN = 60L;
  private void insertData() {
    Transaction op;
    account1 = new Account("Account 1",openingBalance,"Account 1");
    account1.save();
    account2 = new Account("Account 2",openingBalance,"Account 2");
    account2.save();
    op = Transaction.getNewInstance(account1.getId());
    op.amount = new Money(account1.currency,-expense1);
    op.crStatus = CrStatus.CLEARED;
    op.save();
    op.amount = new Money(account1.currency,-expense2);
    op.saveAsNew();
    op.amount = new Money(account1.currency,income1);
    op.saveAsNew();
    op.amount = new Money(account1.currency,income2);
    op.saveAsNew();
    op = Transfer.getNewInstance(account1.getId(),account2.getId());
    op.amount = new Money(account1.currency,transferP);
    op.save();
    op.amount = new Money(account1.currency,-transferN);
    op.saveAsNew();

  }
  
  public void testAccount() {
    Account account,restored = null;
    Long openingBalance = (long) 100;
    account = new Account("TestAccount",openingBalance,"Testing with Junit");
    account.setCurrency("EUR");
    assertEquals("EUR", account.currency.getCurrencyCode());
    account.save();
    assertTrue(account.getId() > 0);
    restored = Account.getInstanceFromDb(account.getId());
    assertEquals(account,restored);
    Long trAmount = (long) 100;
    Transaction op1 = Transaction.getNewInstance(account.getId());
    op1.amount = new Money(account.currency,trAmount);
    op1.comment = "test transaction";
    op1.save();
    assertEquals(account.getTotalBalance().getAmountMinor().longValue(),openingBalance+trAmount);
    Account.delete(account.getId());
    assertNull("Account deleted, but can still be retrieved",Account.getInstanceFromDb(account.getId()));
    assertNull("Account delete should delete transaction, but operation can still be retrieved",Transaction.getInstanceFromDb(op1.getId()));
  }
  /**
   * we test if the db calculates the aggregate sums correctly
   * this is rather a test of the cursor exposed through the content provider
   * but set up is easier through models
   */
  public void testAggregates() {
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        null,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );
    //the database setup creates the default account
    assertEquals(1, cursor.getCount());
    insertData();
    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        Account.PROJECTION_FULL,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assertEquals(3, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    // Since no projection was used, get the column indexes of the returned columns
    int incomeIndex = cursor.getColumnIndex(KEY_SUM_INCOME);
    int expensesIndex = cursor.getColumnIndex(KEY_SUM_EXPENSES);
    int transferIndex = cursor.getColumnIndex(KEY_SUM_TRANSFERS);
    int balanceIndex = cursor.getColumnIndex(KEY_CURRENT_BALANCE);
    assertEquals(income1+income2, cursor.getLong(incomeIndex));
    assertEquals(-expense1-expense2, cursor.getLong(expensesIndex));
    assertEquals(transferP-transferN, cursor.getLong(transferIndex));
    assertEquals(openingBalance+income1+income2-expense1-expense2+transferP-transferN, cursor.getLong(balanceIndex));
    assertTrue(cursor.moveToNext());
    assertEquals(0L, cursor.getLong(incomeIndex));
    assertEquals(0L, cursor.getLong(expensesIndex));
    assertEquals(transferN-transferP, cursor.getLong(transferIndex));
    assertEquals(openingBalance+transferN-transferP, cursor.getLong(balanceIndex));
  }
  public void testGetInstanceZeroReturnsAccount () {
    //even without inserting, there should be always an account in the database
    //insertData();
    assertNotNull("getInstanceFromDb(0) should return an account",Account.getInstanceFromDb(0));
  }
  public void testGetAggregateAccountFromDb () {
    insertData();
    Account.clear();
    String currency = Account.getLocaleCurrency().getCurrencyCode();
    Cursor c = getMockContentResolver().query(
        TransactionProvider.CURRENCIES_URI,
        new String[]{KEY_ROWID},
        KEY_CODE + " = ?",
        new String[]{currency},
        null);
    c.moveToFirst();
    long id = 0 - c.getLong(0);
    c.close();
    AggregateAccount aa =  (AggregateAccount) Account.getInstanceFromDb(id);
    assertEquals(currency,aa.currency.getCurrencyCode());
    assertEquals(openingBalance.longValue()*2,aa.openingBalance.getAmountMinor().longValue());
  }
  public void testBalanceWithoutReset() {
    insertData();
    Money initialclearedBalance = account1.getClearedBalance();
    assertFalse(initialclearedBalance.equals(account1.getReconciledBalance()));
    assertEquals(4,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(0,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    account1.balance(false);
    assertEquals(0,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(4,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    assertEquals(initialclearedBalance,account1.getReconciledBalance());
  }

  public void testBalanceWithReset() {
    insertData();
    Money initialclearedBalance = account1.getClearedBalance();
    assertFalse(initialclearedBalance.equals(account1.getReconciledBalance()));
    assertEquals(4,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.CLEARED.name() + "'"));
    assertEquals(0,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.RECONCILED.name() + "'"));
    account1.balance(true);
    assertEquals(0,count(account1.getId(),KEY_CR_STATUS + " != '" + CrStatus.UNRECONCILED.name() + "'"));
    assertEquals(2,count(account1.getId(),KEY_CR_STATUS + " = '" + CrStatus.UNRECONCILED.name() + "'"));
    assertEquals(initialclearedBalance,account1.getReconciledBalance());
    
  }
  public void testReset() {
    insertData();
    Money initialtotalBalance = account1.getTotalBalance();
    assertEquals(6,count(account1.getId(),null));
    account1.reset(false);
    Account.clear();
    assertEquals(0,count(account1.getId(),null));
    Account resetAccount = Account.getInstanceFromDb(account1.getId());
    assertEquals(initialtotalBalance,resetAccount.getTotalBalance());
  }
  /**
   * @param accountId
   * @param condition if not null interpreted as a where clause for filtering the transactions
   * @return the number of transactions in an account
   */
  private int count(long accountId, String condition) {
    String selection = KEY_ACCOUNTID + " = ?";
    if (condition != null) {
      selection += " AND " + condition;
    }
    Cursor c = getMockContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI, 
        new String[] {"count(*)"},
        selection,
        new String[]{String.valueOf(accountId)},
        null
    );
    c.moveToFirst();
    int result = c.getInt(0);
    c.close();
    return result;
  }
}
