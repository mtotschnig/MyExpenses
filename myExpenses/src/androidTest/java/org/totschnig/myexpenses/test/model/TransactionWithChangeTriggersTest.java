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

import static org.totschnig.myexpenses.db2.RepositoryPartyKt.requireParty;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.model2.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.MoreDbUtilsKt;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.InstrumentationRegistryUtilsKt;
import org.totschnig.myexpenses.testutils.ModelTest;
import org.totschnig.myexpenses.ui.DisplayParty;

import java.util.Date;

import kotlin.Unit;

/**
 * copy of {@link TransactionTest} which runs under the assumption that changes triggers fire
 */
public class TransactionWithChangeTriggersTest extends ModelTest {
  private Account mAccount1;
  private Account mAccount2;
  private Account mAccount3;
  private long catId1;
  private long catId2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mAccount1 = buildAccount("TestAccount 1", 100, "DEBUG");
    mAccount2 = buildAccount("TestAccount 2", 100, "DEBUG");
    mAccount3 = buildAccount("TestAccount 3", 100, "DEBUG");
    ContentValues values = new ContentValues(1);
    values.put(DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL, 1);
    MoreDbUtilsKt.update(getProvider().getOpenHelperForTest().getWritableDatabase(), DatabaseConstants.TABLE_ACCOUNTS, values, null, null);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    InstrumentationRegistryUtilsKt.cleanup(() -> {
      deleteAccount(mAccount1.getId());
      deleteAccount(mAccount2.getId());
      deleteAccount(mAccount3.getId());
      deleteCategory(catId1);
      deleteCategory(catId2);
      return Unit.INSTANCE;
    });
  }

  public void testTransaction() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    String payee = "N.N";
    long start = getRepository().getSequenceCount();
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit);
    op1.setAmount(new Money(currencyUnit, 100L));
    op1.setComment("test transaction");
    op1.setParty(new DisplayParty(requireParty(getRepository(), payee), payee, null));
    op1.save(getContentResolver());
    assertTrue(op1.getId() > 0);
    assertEquals(start  + 1, getRepository().getSequenceCount());
    //save creates a payee as side effect
    assertEquals(1, countPayee(payee));
    Transaction restored = getTransactionFromDb(op1.getId());
    assertEquals(op1, restored);

    Long id = op1.getId();
    getRepository().deleteTransaction(id, false, false);
    //Transaction sequence should report on the number of transactions that have been created
    assertEquals(start  + 1,  getRepository().getSequenceCount());
    assertNull("Transaction deleted, but can still be retrieved", getTransactionFromDb(id));
    op1.saveAsNew(getContentResolver());
    assertNotSame(op1.getId(), id);
    //the payee is still the same, so there should still be only one
    assertEquals(1, countPayee(payee));
  }

  public void testTransfer() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    Transfer op = Transfer.getNewInstance(mAccount1.getId(), currencyUnit, mAccount2.getId());
    Transfer peer;
    op.setAmount(new Money(currencyUnit, 100));
    op.setComment("test transfer");
    op.save(getContentResolver());
    assertTrue(op.getId() > 0);
    Transaction restored = getTransactionFromDb(op.getId());
    assertEquals(op, restored);
    peer = (Transfer) getTransactionFromDb(op.getTransferPeer());
    assertEquals(peer.getId(), op.getTransferPeer().longValue());
    assertEquals(op.getId(), peer.getTransferPeer().longValue());
    assertEquals(op.getTransferAccountId().longValue(), peer.getAccountId());
    getRepository().deleteTransaction(op.getId(), false, false);
    assertNull("Transaction deleted, but can still be retrieved", getTransactionFromDb(op.getId()));
    assertNull("Transfer delete should delete peer, but peer can still be retrieved", getTransactionFromDb(peer.getId()));
  }

  public void testTransferChangeAccounts() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    Transfer op = Transfer.getNewInstance(mAccount1.getId(), currencyUnit, mAccount2.getId());
    op.setAmount(new Money(currencyUnit, 100));
    op.setComment("test transfer");
    assertNotNull(op.save(getContentResolver()));
    op.setAccountId(mAccount2.getId());
    op.setTransferAccountId(mAccount3.getId());
    assertNotNull(op.save(getContentResolver()));
    Transaction restored = getTransactionFromDb(op.getId());
    assertEquals(restored.getAccountId(), mAccount2.getId());
    assertEquals(restored.getTransferAccountId().longValue(), mAccount3.getId());
    assertEquals(restored.getUuid(), op.getUuid());
    Transaction peer = getTransactionFromDb(op.getTransferPeer());
    assertEquals(peer.getAccountId(), mAccount3.getId());
    assertEquals(peer.getTransferAccountId().longValue(), mAccount2.getId());
    assertEquals(peer.getUuid(), op.getUuid());
  }

  /**
   * we test if split parts get the date of their parent
   */
  public void testSplit() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    SplitTransaction op1 = SplitTransaction.getNewInstance(getContentResolver(), mAccount1.getId(), currencyUnit, false);
    op1.setAmount(new Money(currencyUnit, 100L));
    op1.setComment("test transaction");
    op1.setDate(new Date(System.currentTimeMillis() - 1003900000));
    op1.save(getContentResolver());
    assertTrue(op1.getId() > 0);
    Transaction split1 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit, op1.getId());
    split1.setAmount(new Money(currencyUnit, 50L));
    assertEquals(split1.getParentId().longValue(), op1.getId());
    split1.setStatus(STATUS_UNCOMMITTED);
    split1.save(getContentResolver());
    assertTrue(split1.getId() > 0);
    Transaction split2 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit, op1.getId());
    split2.setAmount(new Money(currencyUnit, 50L));
    assertEquals(split2.getParentId().longValue(), op1.getId());
    split2.setStatus(STATUS_UNCOMMITTED);
    split2.save(getContentResolver());
    assertTrue(split2.getId() > 0);
    op1.save(getContentResolver());
    //we expect the parent to make sure that parts have the same date
    Transaction restored = getTransactionFromDb(op1.getId());
    assertEquals(op1, restored);
    Transaction split1Restored = getTransactionFromDb(split1.getId());
    assertEquals(restored.getDate(), split1Restored.getDate());
    Transaction split2Restored = getTransactionFromDb(split2.getId());
    assertEquals(restored.getDate(), split2Restored.getDate());
    restored.setCrStatus(CrStatus.CLEARED);
    restored.save(getContentResolver());
    //splits should not be touched by simply saving the parent
    assertNotNull("Split parts deleted after saving parent", getTransactionFromDb(split1.getId()));
    assertNotNull("Split parts deleted after saving parent", getTransactionFromDb(split2.getId()));
  }

  public void testDeleteSplitWithPartTransfer() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    SplitTransaction op1 = SplitTransaction.getNewInstance(getContentResolver(), mAccount1.getId(), currencyUnit, false);
    Money money = new Money(currencyUnit, 100L);
    op1.setAmount(money);
    op1.save(getContentResolver());
    Transaction split1 = new Transfer(mAccount1.getId(), money, mAccount2.getId(), op1.getId());
    split1.save(getContentResolver());
    getRepository().deleteTransaction(op1.getId(), false, false);
    assertNull("Transaction deleted, but can still be retrieved", getTransactionFromDb(op1.getId()));
  }

  public void testIncreaseCatUsage() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    catId1 = writeCategory("Test category 1", null);
    catId2 = writeCategory("Test category 2", null);
    assertEquals(getCatUsage(catId1), 0);
    assertEquals(getCatUsage(catId2), 0);
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit);
    op1.setAmount(new Money(currencyUnit, 100L));
    op1.setCatId(catId1);
    op1.save(getContentResolver());
    //saving a new transaction increases usage
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 0);
    //updating a transaction without touching catId does not increase usage
    op1.setComment("Now with comment");
    op1.save(getContentResolver());
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 0);
    //updating category in transaction, does increase usage of new catId
    op1.setCatId(catId2);
    op1.save(getContentResolver());
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 1);
    //new transaction without cat, does not increase usage
    Transaction op2 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit);
    op2.setAmount(new Money(currencyUnit, 100L));
    op2.save(getContentResolver());
    assertEquals(getCatUsage(catId1), 1);
    assertEquals(getCatUsage(catId2), 1);
    //setting catId now does increase usage
    op2.setCatId(catId1);
    op2.save(getContentResolver());
    assertEquals(getCatUsage(catId1), 2);
    assertEquals(getCatUsage(catId2), 1);
  }

  public void testIncreaseAccountUsage() {
    CurrencyUnit currencyUnit = getHomeCurrency();
    assertEquals(0, getAccountUsage(mAccount1.getId()));
    assertEquals(0, getAccountUsage(mAccount2.getId()));
    Transaction op1 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit);
    op1.setAmount(new Money(currencyUnit, 100L));
    op1.save(getContentResolver());
    assertEquals(1, getAccountUsage(mAccount1.getId()));
    //transfer
    Transfer op2 = Transfer.getNewInstance(mAccount1.getId(), currencyUnit, mAccount2.getId());
    op2.setAmount(new Money(currencyUnit, 100L));
    op2.save(getContentResolver());
    assertEquals(2, getAccountUsage(mAccount1.getId()));
    assertEquals(1, getAccountUsage(mAccount2.getId()));
    op1.setAccountId(mAccount2.getId());
    op1.save(getContentResolver());
    assertEquals(2, getAccountUsage(mAccount2.getId()));
    //split
    SplitTransaction op3 = SplitTransaction.getNewInstance(getContentResolver(), mAccount1.getId(), currencyUnit, false);
    op3.setAmount(new Money(currencyUnit, 100L));
    op3.save(getContentResolver());
    Transaction split1 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit, op3.getId());
    split1.setAmount(new Money(currencyUnit, 50L));
    split1.setStatus(STATUS_UNCOMMITTED);
    split1.save(getContentResolver());
    Transaction split2 = Transaction.getNewInstance(mAccount1.getId(), currencyUnit, op3.getId());
    split2.setAmount(new Money(currencyUnit, 50L));
    split2.setStatus(STATUS_UNCOMMITTED);
    split2.save(getContentResolver());
    assertEquals(3, getAccountUsage(mAccount1.getId()));
  }

  private int countPayee(String name) {
    Cursor cursor = getMockContentResolver().query(TransactionProvider.PAYEES_URI, new String[]{"count(*)"},
        "name = ?", new String[]{name}, null);
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
