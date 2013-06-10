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
import org.totschnig.myexpenses.model.DataObjectNotFoundException;

public class AccountTest extends ModelTest  {
  
  public void test_Account() {
    Account account,restored = null;
    account = new Account("TestAccount",100,"Testing with Junit");
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
  }
}
