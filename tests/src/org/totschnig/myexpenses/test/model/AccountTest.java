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

public class AccountTest extends ModelTest  {
  
  public void test_Account() {
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
}
