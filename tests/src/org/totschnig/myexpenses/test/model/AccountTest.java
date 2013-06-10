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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.test.ProviderTestCase2;


import junit.framework.Assert;

public class AccountTest extends  ProviderTestCase2<TransactionProvider>  {

  public AccountTest() {
    super(TransactionProvider.class,TransactionProvider.AUTHORITY);
}

  public Account mAccount;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ((MyApplication) getContext().getApplicationContext()).mockCr = getMockContentResolver();
    mAccount = new Account("TestAccount",100,"Testing with Junit");
  }
  
  public void testAccount() {
    mAccount.setCurrency("EUR");
    Assert.assertEquals("EUR", mAccount.currency.getCurrencyCode());
    mAccount.save();
    Assert.assertTrue(mAccount.id > 0);
  }
}
