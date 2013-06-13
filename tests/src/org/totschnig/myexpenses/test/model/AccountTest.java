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

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;

public class AccountTest extends ModelTest  {
  
  public void testAccount() {
    Account account,restored = null;
    Long openingBalance = (long) 100;
    account = new Account("TestAccount",openingBalance,"Testing with Junit");
    account.setCurrency("EUR");
    assertEquals("EUR", account.currency.getCurrencyCode());
    account.save();
    assertTrue(account.id > 0);
    try {
      restored = Account.getInstanceFromDb(account.id);
    } catch (DataObjectNotFoundException e) {
      fail("Account saved, but could not be retrieved");
    }
    assertEquals(account,restored);
    Long trAmount = (long) 100;
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account.id);
    op1.amount = new Money(account.currency,trAmount);
    op1.comment = "test transaction";
    op1.save();
    assertEquals(account.getCurrentBalance().getAmountMinor().longValue(),openingBalance+trAmount);
    Account.delete(account.id);
    try {
      Account.getInstanceFromDb(account.id);
      fail("Account deleted, but can still be retrieved");
    } catch (DataObjectNotFoundException e) {
      //succeed
    }
    try {
      Transaction.getInstanceFromDb(op1.id);
      fail("Account delete should delete transaction, but operation can still be retrieved");
    } catch (DataObjectNotFoundException e) {
      //succeed
    }
  }
  /**
   * we test if the db calculates the aggregate sums correctly
   * this is rather a test of the cursor exposed through the content provider
   * but set up is easier through models
   */
  public void testAggregates() {
    Account account1, account2;
    Transaction op;
    Long openingBalance = 100L,
        expense1 = 10L,
        expense2 = 20L,
        income1 = 30L,
        income2 = 40L,
        transferP = 50L,
        transferN = 60L;

    account1 = new Account("Account 1",openingBalance,"Account 1");
    account1.save();
    account2 = new Account("Account 2",openingBalance,"Account 2");
    account2.save();
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op.amount = new Money(account1.currency,-expense1);
    op.save();
    op.amount = new Money(account1.currency,-expense2);
    op.saveAsNew();
    op.amount = new Money(account1.currency,income1);
    op.saveAsNew();
    op.amount = new Money(account1.currency,income2);
    op.saveAsNew();
    op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,account1.id);
    op.transfer_account = account2.id;
    op.amount = new Money(account1.currency,transferP);
    op.save();
    op.amount = new Money(account1.currency,-transferN);
    op.saveAsNew();
    Cursor cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        null,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assertEquals(2, cursor.getCount());

    assertTrue(cursor.moveToFirst());

    // Since no projection was used, get the column indexes of the returned columns
    int incomeIndex = cursor.getColumnIndex("sum_income");
    int expensesIndex = cursor.getColumnIndex("sum_expenses");
    int transferIndex = cursor.getColumnIndex("sum_transfer");
    int balanceIndex = cursor.getColumnIndex("current_balance");
    assertEquals(income1+income2, cursor.getLong(incomeIndex));
    assertEquals(expense1+expense2, cursor.getLong(expensesIndex));
    assertEquals(transferP-transferN, cursor.getLong(transferIndex));
    assertEquals(openingBalance+income1+income2-expense1-expense2+transferP-transferN, cursor.getLong(balanceIndex));
    assertTrue(cursor.moveToNext());
    assertEquals(0L, cursor.getLong(incomeIndex));
    assertEquals(0L, cursor.getLong(expensesIndex));
    assertEquals(transferN-transferP, cursor.getLong(transferIndex));
    assertEquals(openingBalance+transferN-transferP, cursor.getLong(balanceIndex));
  }
}
