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
import org.totschnig.myexpenses.Money;
import org.totschnig.myexpenses.MyExpenses;
import org.totschnig.myexpenses.Template;
import org.totschnig.myexpenses.Transaction;
import org.totschnig.myexpenses.Transfer;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TransactionTest extends TestCase {
  private Currency currency;
  private Account mAccount1;
  private Account mAccount2;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      currency = Currency.getInstance(Locale.getDefault());
      mAccount1 = new Account("TestAccount 1",100,"Main account",currency);
      mAccount1.save();
      mAccount2 = new Account("TestAccount 2",100,"Secondary account",currency);
      mAccount2.save();
  }
  public void testTransfer() {
    Transfer op = (Transfer) Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,mAccount1.id);
    op.catId = mAccount2.id;
    op.amount = new Money(currency,(long) 100);
    op.comment = "test transfer";
    op.save();
    Assert.assertTrue(op.id > 0);
    Transfer peer = (Transfer) Transaction.getInstanceFromDb(op.transfer_peer);
    Assert.assertEquals(op.id, peer.transfer_peer);
  }
  public void testTemplate() {
    Long start = mAccount1.getCurrentBalance().getAmountMinor();
    Long amount = (long) 100;
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id);
    op1.amount = new Money(currency,amount);
    op1.comment = "test transfer";
    op1.save();
    Assert.assertEquals(mAccount1.getCurrentBalance().getAmountMinor().longValue(), start+amount);
    Template t = new Template(op1,"Template");
    t.save();
    Transaction op2  = Transaction.getInstanceFromTemplate(t.id);
    op2.save();
    Assert.assertEquals(mAccount1.getCurrentBalance().getAmountMinor().longValue(), start+2*amount);
  }
  @Override
  protected void tearDown() throws Exception {
    Account.delete(mAccount1.id);
    Account.delete(mAccount2.id);
  }
}
