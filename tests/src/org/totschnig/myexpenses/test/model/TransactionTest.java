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
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;

public class TransactionTest extends ModelTest  {
  private Account mAccount1;
  private Account mAccount2;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mAccount1 = new Account("TestAccount 1",100,"Main account");
      mAccount1.save();
      mAccount2 = new Account("TestAccount 2",100,"Secondary account");
      mAccount2.save();
  }
  public void testTemplate() {
    Long start = mAccount1.getCurrentBalance().getAmountMinor();
    Long amount = (long) 100;
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id);
    op1.amount = new Money(mAccount1.currency,amount);
    op1.comment = "test transaction";
    op1.save();
    assertEquals(mAccount1.getCurrentBalance().getAmountMinor().longValue(), start+amount);
    Template t = new Template(op1,"Template");
    t.save();
    Transaction op2  = Transaction.getInstanceFromTemplate(t.id);
    op2.save();
    assertEquals(mAccount1.getCurrentBalance().getAmountMinor().longValue(), start+2*amount);
    Transaction restored;
    try {
      restored = Transaction.getInstanceFromDb(op2.id);
      assertEquals(op2,restored);
    } catch (DataObjectNotFoundException e) {
       fail("Could not restore transaction");
    }
    Template.delete(t.id);
    try {
      Template.getInstanceFromDb(t.id);
      fail("Template deleted, but can still be retrieved");
    } catch (DataObjectNotFoundException e) {
      //succeed
    }
  }
  public void testTransaction() {
    assertEquals(0, Transaction.getTransactionSequence());
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id);
    op1.amount = new Money(mAccount1.currency,100L);
    op1.comment = "test transaction";
    op1.save();
    assertTrue(op1.id > 0);
    assertEquals(1, Transaction.getTransactionSequence());
    try {
      Transaction restored = Transaction.getInstanceFromDb(op1.id);
      assertEquals(op1,restored);
    } catch (DataObjectNotFoundException e) {
      fail("Could not restore transaction");
    }
    Transaction.delete(op1.id);
    //Transaction sequence should report on the number of transactions that have been created
    assertEquals(1, Transaction.getTransactionSequence());
    try {
      Transaction.getInstanceFromDb(op1.id);
      fail("Transaction deleted, but can still be retrieved");
    } catch (DataObjectNotFoundException e) {
      //succeed
    }
  }
  
  public void testTransfer() {
    Transfer op = (Transfer) Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER,mAccount1.id);
    Transfer peer;
    op.transfer_account = mAccount2.id;
    op.amount = new Money(mAccount1.currency,(long) 100);
    op.comment = "test transfer";
    op.save();
    assertTrue(op.id > 0);
    try {
      peer = (Transfer) Transaction.getInstanceFromDb(op.transfer_peer);
      assertEquals(peer.id,op.transfer_peer);
      assertEquals(op.id, peer.transfer_peer);
      assertEquals(op.transfer_account, peer.accountId);
      Transaction.delete(op.id);
      try {
        Transaction.getInstanceFromDb(op.id);
        fail("Transaction deleted, but can still be retrieved");
      } catch (DataObjectNotFoundException e) {
        //succeed
      }
      try {
        Transaction.getInstanceFromDb(peer.id);
        fail("Transfer delete should delete peer, but peer can still be retrieved");
      } catch (DataObjectNotFoundException e) {
        //succeed
      }
    } catch (DataObjectNotFoundException e) {
      fail("Could not restore peer for transfer");
    }
  }
}
