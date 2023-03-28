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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.TransactionProvider;

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

  private void insertData() {
    Transaction op;
    account1 = buildAccount("Account 1", openingBalance);
    account2 = buildAccount("Account 2", openingBalance);
    catId = writeCategory(TEST_CAT, null);
    op = Transaction.getNewInstance(account1);
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
    Transfer op1 = Transfer.getNewInstance(account1, account2.getId());
    op1.setAmount(new Money(account1.getCurrencyUnit(), transferP));
    op1.save();
    op1.setAmount(new Money(account1.getCurrencyUnit(), -transferN));
    op1.saveAsNew();
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
    insertData();
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_FULL_URI,  // the URI for the main data table
        null,            // get all the columns
        null,                       // no selection columns, get all the records
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assertEquals(2, cursor.getCount());
    cursor.close();

    cursor = getMockContentResolver().query(
        TransactionProvider.ACCOUNTS_FULL_URI,  // the URI for the main data table
        null,            // get all the columns
        KEY_ROWID + "=" + account1.getId(),
        null,                       // no selection criteria
        null                        // use default the sort order
    );
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
        TransactionProvider.ACCOUNTS_FULL_URI,  // the URI for the main data table
        null,            // get all the columns
        KEY_ROWID + "=" + account2.getId(),
        null,                       // no selection criteria
        null                        // use default the sort order
    );

    assertTrue(cursor.moveToFirst());
    assertEquals(0L, cursor.getLong(incomeIndex));
    assertEquals(0L, cursor.getLong(expensesIndex));
    assertEquals(transferN - transferP, cursor.getLong(transferIndex));
    assertEquals(openingBalance + transferN - transferP, cursor.getLong(balanceIndex));
    cursor.close();
  }


  public void testGetAggregateAccountFromDb() {
    insertData();
    String currency = getHomeCurrency().getCode();
    Cursor c = getMockContentResolver().query(
        TransactionProvider.CURRENCIES_URI,
        new String[]{KEY_ROWID},
        KEY_CODE + " = ?",
        new String[]{currency},
        null);
    c.moveToFirst();
    long id = -c.getLong(0);
    c.close();
    AggregateAccount aa = (AggregateAccount) Account.getInstanceFromDb(id);
    assertEquals(currency, aa.getCurrencyUnit().getCode());
    assertEquals(openingBalance * 2, aa.openingBalance.getAmountMinor());
  }
}
