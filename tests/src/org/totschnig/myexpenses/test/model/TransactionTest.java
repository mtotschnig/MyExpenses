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

import java.util.Date;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;

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
    String payee = "N.N";
    assertEquals(0L, Transaction.getSequenceCount().longValue());
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id);
    op1.amount = new Money(mAccount1.currency,100L);
    op1.comment = "test transaction";
    op1.payee = payee;
    op1.save();
    assertTrue(op1.id > 0);
    assertEquals(1L, Transaction.getSequenceCount().longValue());
    //save creates a payee as side effect
    assertEquals(1,countPayee(payee));
    try {
      Transaction restored = Transaction.getInstanceFromDb(op1.id);
      assertEquals(op1,restored);
    } catch (DataObjectNotFoundException e) {
      fail("Could not restore transaction");
    }
    Long id = op1.id;
    Transaction.delete(id);
    //Transaction sequence should report on the number of transactions that have been created
    assertEquals(1L, Transaction.getSequenceCount().longValue());
    try {
      Transaction.getInstanceFromDb(id);
      fail("Transaction deleted, but can still be retrieved");
    } catch (DataObjectNotFoundException e) {
      //succeed
    }
    op1.saveAsNew();
    assertTrue(op1.id != id);
    //the payee is still the same, so there should still be only one
    assertEquals(1,countPayee(payee));
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
  /**
   * we test if split parts get the date of their parent
   */
  public void testSplit() {
    SplitTransaction op1 = (SplitTransaction) Transaction.getTypedNewInstance(MyExpenses.TYPE_SPLIT,mAccount1.id);
    op1.amount = new Money(mAccount1.currency,100L);
    op1.comment = "test transaction";
    op1.setDate(new Date(System.currentTimeMillis()-1003900000));
    op1.save();
    assertTrue(op1.id > 0);
    Transaction split1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id,op1.id);
    split1.amount = new Money(mAccount1.currency,50L);
    assertTrue(split1.parentId == op1.id);
    split1.status =org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
    split1.save();
    assertTrue(split1.id > 0);
    Transaction split2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount1.id,op1.id);
    split2.amount = new Money(mAccount1.currency,50L);
    assertTrue(split2.parentId == op1.id);
    split2.status = org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
    split2.save();
    assertTrue(split2.id > 0);
    op1.commit();
    //that parents and parts are in consistent state is currently guaranteed by the UI
    //which after saving a parent, commits the parts.
    //we expect the parent to make sure that parts have the same date
    Transaction restored = Transaction.getInstanceFromDb(op1.id);
    assertTrue(restored.getDate().equals(Transaction.getInstanceFromDb(split1.id).getDate()));
    assertTrue(restored.getDate().equals(Transaction.getInstanceFromDb(split2.id).getDate()));
  }
  public int countPayee(String name) {
    Cursor cursor = getMockContentResolver().query(TransactionProvider.PAYEES_URI,new String[] {"count(*)"},
        "name = ?", new String[] {name}, null);
    if (cursor.getCount() == 0) {
      cursor.close();
      return 0;
    } else {
      cursor.moveToFirst();
      int result = cursor.getInt(0);
      cursor.close();
      return result;
    }
  }
}
