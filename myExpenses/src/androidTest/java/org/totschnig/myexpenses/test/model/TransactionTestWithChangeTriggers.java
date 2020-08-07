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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.PictureDirHelper;

import java.util.Date;

import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

/**
 * copy of {@link TransactionTest} which runs under the assumption that changes triggers fire
 */
public class TransactionTestWithChangeTriggers extends ModelTest {
  private Account mAccount1;
  private Account mAccount2;
  private Account mAccount3;
  private long catId1;
  private long catId2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mAccount1 = new Account("TestAccount 1", 100, "Main account");
    mAccount1.setSyncAccountName("DEBUG");
    mAccount1.save();
    mAccount2 = new Account("TestAccount 2", 100, "Secondary account");
    mAccount2.setSyncAccountName("DEBUG");
    mAccount2.save();
    mAccount3 = new Account("TestAccount 3", 100, "Secondary account");
    mAccount3.setSyncAccountName("DEBUG");
    mAccount3.save();
    ContentValues values = new ContentValues(1);
    values.put(DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL, 1);
    getProvider().getOpenHelperForTest().getWritableDatabase().update(DatabaseConstants.TABLE_ACCOUNTS, values, null, null);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Account.delete(mAccount1.getId());
    Account.delete(mAccount2.getId());
    Category.delete(catId1);
    Category.delete(catId2);
  }

  public void testTransaction() {
    String payee = "N.N";
    long start = Transaction.getSequenceCount().longValue();
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId());
    assert op1 != null;
    op1.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op1.setComment("test transaction");
    op1.setPictureUri(PictureDirHelper.getOutputMediaUri(false));//we need an uri that is considered "home"
    op1.setPayee(payee);
    op1.save();
    assertTrue(op1.getId() > 0);
    assertEquals(start  + 1, Transaction.getSequenceCount().longValue());
    //save creates a payee as side effect
    assertEquals(1, countPayee(payee));
    Transaction restored = Transaction.getInstanceFromDb(op1.getId());
    assertEquals(op1, restored);

    Long id = op1.getId();
    Transaction.delete(id, false);
    //Transaction sequence should report on the number of transactions that have been created
    assertEquals(start  + 1, Transaction.getSequenceCount().longValue());
    assertNull("Transaction deleted, but can still be retrieved", Transaction.getInstanceFromDb(id));
    op1.saveAsNew();
    assertNotSame(op1.getId(), id);
    //the payee is still the same, so there should still be only one
    assertEquals(1, countPayee(payee));
  }

  public void testTransfer() {
    Transfer op = Transfer.getNewInstance(mAccount1.getId(), mAccount2.getId());
    Transfer peer;
    assert op != null;
    op.setAmount(new Money(mAccount1.getCurrencyUnit(), (long) 100));
    op.setComment("test transfer");
    op.setPictureUri(PictureDirHelper.getOutputMediaUri(false));
    op.save();
    assertTrue(op.getId() > 0);
    Transaction restored = Transaction.getInstanceFromDb(op.getId());
    assertEquals(op, restored);
    peer = (Transfer) Transaction.getInstanceFromDb(op.getTransferPeer());
    assert peer != null;
    assertEquals(peer.getId(), op.getTransferPeer().longValue());
    assertEquals(op.getId(), peer.getTransferPeer().longValue());
    assertEquals(op.getTransferAccountId().longValue(), peer.getAccountId());
    Transaction.delete(op.getId(), false);
    assertNull("Transaction deleted, but can still be retrieved", Transaction.getInstanceFromDb(op.getId()));
    assertNull("Transfer delete should delete peer, but peer can still be retrieved", Transaction.getInstanceFromDb(peer.getId()));
  }

  public void testTransferChangeAccounts() {
    Transfer op = Transfer.getNewInstance(mAccount1.getId(), mAccount2.getId());
    assertNotNull(op);
    op.setAmount(new Money(mAccount1.getCurrencyUnit(), (long) 100));
    op.setComment("test transfer");
    assertNotNull(op.save());
    op.setAccountId(mAccount2.getId());
    op.setTransferAccountId(mAccount3.getId());
    assertNotNull(op.save());
    Transaction restored = Transaction.getInstanceFromDb(op.getId());
    assertEquals(restored.getAccountId(), mAccount2.getId());
    assertEquals(restored.getTransferAccountId().longValue(), mAccount3.getId());
    assertEquals(restored.getUuid(), op.getUuid());
    Transaction peer = Transaction.getInstanceFromDb(op.getTransferPeer());
    assertEquals(peer.getAccountId(), mAccount3.getId());
    assertEquals(peer.getTransferAccountId().longValue(), mAccount2.getId());
    assertEquals(peer.getUuid(), op.getUuid());
  }

  /**
   * we test if split parts get the date of their parent
   */
  public void testSplit() {
    SplitTransaction op1 = SplitTransaction.getNewInstance(mAccount1.getId(), false);
    assert op1 != null;
    op1.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op1.setComment("test transaction");
    op1.setPictureUri(PictureDirHelper.getOutputMediaUri(false));
    op1.setDate(new Date(System.currentTimeMillis() - 1003900000));
    op1.save();
    assertTrue(op1.getId() > 0);
    Transaction split1 = Transaction.getNewInstance(mAccount1.getId(), op1.getId());
    assert split1 != null;
    split1.setAmount(new Money(mAccount1.getCurrencyUnit(), 50L));
    assertEquals(split1.getParentId().longValue(), op1.getId());
    split1.setStatus(STATUS_UNCOMMITTED);
    split1.save();
    assertTrue(split1.getId() > 0);
    Transaction split2 = Transaction.getNewInstance(mAccount1.getId(), op1.getId());
    assert split2 != null;
    split2.setAmount(new Money(mAccount1.getCurrencyUnit(), 50L));
    assertEquals(split2.getParentId().longValue(), op1.getId());
    split2.setStatus(STATUS_UNCOMMITTED);
    split2.save();
    assertTrue(split2.getId() > 0);
    op1.save();
    //we expect the parent to make sure that parts have the same date
    Transaction restored = Transaction.getInstanceFromDb(op1.getId());
    assertEquals(op1, restored);
    assert restored != null;
    Transaction split1Restored = Transaction.getInstanceFromDb(split1.getId());
    assert split1Restored != null;
    assertEquals(restored.getDate(), split1Restored.getDate());
    Transaction split2Restored = Transaction.getInstanceFromDb(split2.getId());
    assert split2Restored != null;
    assertEquals(restored.getDate(), split2Restored.getDate());
    restored.setCrStatus(CrStatus.CLEARED);
    restored.save();
    //splits should not be touched by simply saving the parent
    assertNotNull("Split parts deleted after saving parent", Transaction.getInstanceFromDb(split1.getId()));
    assertNotNull("Split parts deleted after saving parent", Transaction.getInstanceFromDb(split2.getId()));
  }

  public void testDeleteSplitWithPartTransfer() {
    SplitTransaction op1 = SplitTransaction.getNewInstance(mAccount1.getId(), false);
    assert op1 != null;
    Money money = new Money(mAccount1.getCurrencyUnit(), 100L);
    op1.setAmount(money);
    op1.save();
    Transaction split1 = new Transfer(mAccount1.getId(), money, mAccount2.getId(), op1.getId());
    split1.save();
    Transaction.delete(op1.getId(), false);
    assertNull("Transaction deleted, but can still be retrieved", Transaction.getInstanceFromDb(op1.getId()));
  }

  public void testIncreaseCatUsage() {
    catId1 = Category.write(0, "Test category 1", null);
    catId2 = Category.write(0, "Test category 2", null);
    assertEquals(getCatUsage(catId1), 0);
    assertEquals(getCatUsage(catId2), 0);
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId());
    assert op1 != null;
    op1.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op1.setCatId(catId1);
    op1.save();
    //saving a new transaction increases usage
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 0);
    //updating a transaction without touching catId does not increase usage
    op1.setComment("Now with comment");
    op1.save();
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 0);
    //updating category in transaction, does increase usage of new catId
    op1.setCatId(catId2);
    op1.save();
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 1);
    //new transaction without cat, does not increase usage
    Transaction op2 = Transaction.getNewInstance(mAccount1.getId());
    assert op2 != null;
    op2.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op2.save();
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 1);
    //setting catId now does increase usage
    op2.setCatId(catId1);
    op2.save();
    assertEquals(getCatUsage(catId1), 2);
    assertEquals(getCatUsage(catId2), 1);
  }

  public void testIncreaseAccountUsage() {
    assertEquals(0, getAccountUsage(mAccount1.getId()));
    assertEquals(0, getAccountUsage(mAccount2.getId()));
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId());
    assert op1 != null;
    op1.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op1.save();
    assertEquals(1, getAccountUsage(mAccount1.getId()));
    //transfer
    Transfer op2 = Transfer.getNewInstance(mAccount1.getId(), mAccount2.getId());
    assert op2 != null;
    op2.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op2.save();
    assertEquals(2, getAccountUsage(mAccount1.getId()));
    assertEquals(1, getAccountUsage(mAccount2.getId()));
    op1.setAccountId(mAccount2.getId());
    op1.save();
    assertEquals(2, getAccountUsage(mAccount2.getId()));
    //split
    SplitTransaction op3 = SplitTransaction.getNewInstance(mAccount1.getId(), false);
    assert op3 != null;
    op3.setAmount(new Money(mAccount1.getCurrencyUnit(), 100L));
    op3.save();
    Transaction split1 = Transaction.getNewInstance(mAccount1.getId(), op3.getId());
    assert split1 != null;
    split1.setAmount(new Money(mAccount1.getCurrencyUnit(), 50L));
    split1.setStatus(STATUS_UNCOMMITTED);
    split1.save();
    Transaction split2 = Transaction.getNewInstance(mAccount1.getId(), op3.getId());
    assert split2 != null;
    split2.setAmount(new Money(mAccount1.getCurrencyUnit(), 50L));
    split2.setStatus(STATUS_UNCOMMITTED);
    split2.save();
    op3.save();
    assertEquals(3, getAccountUsage(mAccount1.getId()));
  }

  private int countPayee(String name) {
    Cursor cursor = getMockContentResolver().query(TransactionProvider.PAYEES_URI, new String[]{"count(*)"},
        "name = ?", new String[]{name}, null);
    assert cursor != null;
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

  private long getCatUsage(long catId) {
    return getUsage(catId, TransactionProvider.CATEGORIES_URI);
  }

  private long getAccountUsage(long acccountId) {
    return getUsage(acccountId, TransactionProvider.ACCOUNTS_URI);
  }

  private long getUsage(long catId, Uri baseUri) {
    long result = 0;
    Cursor c = getMockContentResolver().query(
        baseUri.buildUpon().appendPath(String.valueOf(catId)).build(),
        new String[]{DatabaseConstants.KEY_USAGES},
        null, null, null);
    if (c != null) {
      if (c.moveToFirst()) {
        result = c.getLong(0);
      }
      c.close();
    }
    return result;
  }
}
