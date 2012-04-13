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

package org.totschnig.myexpenses.test;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.Account;

import junit.framework.Assert;
import android.test.AndroidTestCase;

public class AccountTest extends AndroidTestCase {
  public Account mAccount;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mAccount = new Account("TestAccount",100,"Testing with Junit",Currency.getInstance(Locale.getDefault()));
  }
  public void testAccount() {
    mAccount.setCurrency("EUR");
    Assert.assertEquals("EUR", mAccount.currency.getCurrencyCode());
    mAccount.save();
    Assert.assertTrue(mAccount.id > 0);
  }
  @Override
  protected void tearDown() throws Exception {
    Account.delete(mAccount.id);
  }
}
