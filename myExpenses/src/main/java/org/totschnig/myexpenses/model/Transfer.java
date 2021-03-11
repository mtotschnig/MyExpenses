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

package org.totschnig.myexpenses.model;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;

import org.totschnig.myexpenses.util.Preconditions;

import java.util.ArrayList;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE;

/**
 * a transfer consists of a pair of transactions, one for each account
 * this class handles creation and update
 *
 * @author Michael Totschnig
 */
public class Transfer extends Transaction implements ITransfer {

  public static final String RIGHT_ARROW = "▶";
  public static final String LEFT_ARROW = "◀";
  public static final String BI_ARROW = "⇄";

  private Long transferPeer;
  private Long transferAccountId;

  public Long getTransferPeer() {
    return transferPeer;
  }

  public void setTransferPeer(Long transferPeer) {
    this.transferPeer = transferPeer;
  }

  public Long getTransferAccountId() {
    return transferAccountId;
  }

  @Override
  public void setTransferAccountId(Long transferAccountId) {
    this.transferAccountId = transferAccountId;
  }

  public Transfer() {
    super();
  }

  public Transfer(long accountId, Money amount) {
    this(accountId, amount, null);
  }

  public Transfer(long accountId, Money amount, Long transferAccountId) {
    this(accountId, amount, transferAccountId, null);
  }


  public Transfer(long accountId, Long transferAccountId, Long parentId) {
    super(accountId, parentId);
    setTransferAccountId(transferAccountId);
  }

  public Transfer(long accountId, Money amount, Long transferAccountId, Long parentId) {
    super(accountId, amount);
    setTransferAccountId(transferAccountId);
    setParentId(parentId);
  }

  @Override
  public void setAmount(Money amount) {
    super.setAmount(amount);
    this.setTransferAmount(new Money(amount.getCurrencyUnit(), amount.getAmountMajor().negate()));
  }

  public void setAmountAndTransferAmount(Money amount, Money transferAmount) {
    super.setAmount(amount);
    this.setTransferAmount(transferAmount);
  }

  public static Transfer getNewInstance(long accountId) {
    return getNewInstance(accountId, null);
  }

  /**
   * @param accountId         if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
   * @param transferAccountId
   * @return
   */
  public static Transfer getNewInstance(long accountId, Long transferAccountId) {
    return getNewInstance(accountId, transferAccountId, null);
  }

  public static Transfer getNewInstance(long accountId, Long transferAccountId, Long parentId) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    if (transferAccountId != null) {
      Account transferAccount = Account.getInstanceFromDbWithFallback(transferAccountId);
      if (transferAccount == null) {
        return null;
      }
    }
    return new Transfer(accountId, new Money(account.getCurrencyUnit(), 0L), transferAccountId, parentId);
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset, boolean callerIsSyncAdapter, boolean withCommit) {
    Uri uri = getUriForSave(callerIsSyncAdapter);
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    long amount = this.getAmount().getAmountMinor();
    long transferAmount = this.getTransferAmount().getAmountMinor();
    //the id of the peer_account is stored in KEY_TRANSFER_ACCOUNT,
    //the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, getComment());
    initialValues.put(KEY_DATE, getDate());
    initialValues.put(KEY_VALUE_DATE, getValueDate());
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_TRANSFER_ACCOUNT, getTransferAccountId());
    initialValues.put(KEY_CR_STATUS, getCrStatus().name());
    initialValues.put(KEY_ACCOUNTID, getAccountId());
    savePicture(initialValues);
    if (getId() == 0) {
      //both parts of the transfer share uuid
      initialValues.put(KEY_UUID, requireUuid());
      initialValues.put(KEY_PARENTID, getParentId());
      initialValues.put(KEY_STATUS, getStatus());
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
      if (parentOffset != -1) {
        builder.withValueBackReference(KEY_PARENTID, parentOffset);
      }
      long transferPeer = Transaction.findByAccountAndUuid(getTransferAccountId(), getUuid());
      if (transferPeer > -1) {
        initialValues.put(KEY_TRANSFER_PEER, transferPeer);
      }
      ops.add(builder.withValues(initialValues).build());
      if (transferPeer > -1) {
        //a transaction might have been locally transformed from a transfer to a normal transaction
        //if the transfer account is deleted. If later the transfer account is synced again, this
        //peer would still exist, and prevent recreation of the transfer. What we do here, is relink
        //the two.
        //Now if two parts of a transfer are both synced, we create first a transaction, and when we
        //later sync the second account, we link the two peers
        ContentValues transferValues = new ContentValues();
        transferValues.put(KEY_TRANSFER_ACCOUNT, getAccountId());
        ops.add(ContentProviderOperation.newUpdate(uri)
            .withSelection(KEY_ROWID + " = ?", new String[]{String.valueOf(transferPeer)})
            .withValues(transferValues).withValueBackReference(KEY_TRANSFER_PEER, offset)
            .build());
      } else {
        ContentValues transferValues = new ContentValues(initialValues);
        //if the transfer is part of a split, the transfer peer needs to have a null parent
        transferValues.remove(KEY_PARENTID);
        transferValues.put(KEY_AMOUNT, transferAmount);
        transferValues.put(KEY_TRANSFER_ACCOUNT, getAccountId());
        transferValues.put(KEY_ACCOUNTID, getTransferAccountId());
        ops.add(ContentProviderOperation.newInsert(uri)
            .withValues(transferValues).withValueBackReference(KEY_TRANSFER_PEER, offset)
            .build());
        //we have to set the transferPeer for the first transaction
        ops.add(ContentProviderOperation.newUpdate(uri)
            .withValueBackReference(KEY_TRANSFER_PEER, offset + 1)
            .withSelection(KEY_ROWID + " = ?", new String[]{""})//replaced by back reference
            .withSelectionBackReference(0, offset)
            .build());
      }
    } else {
      //we set the transfer peers uuid to null initially to prevent violation of unique index which
      //happens if the account after update is identical to transferAccountId before update
      ContentValues uuidNullValues = new ContentValues(1);
      uuidNullValues.putNull(KEY_UUID);
      Uri transferUri = uri.buildUpon().appendPath(String.valueOf(getTransferPeer())).build();
      ops.add(ContentProviderOperation
          .newUpdate(transferUri)
          .withValues(uuidNullValues).build());
      ops.add(ContentProviderOperation
          .newUpdate(uri.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValues(initialValues).build());
      ContentValues transferValues = new ContentValues(initialValues);
      transferValues.remove(KEY_VALUE_DATE);
      transferValues.put(KEY_AMOUNT, transferAmount);
      //if the user has changed the account to which we should transfer,
      //in the peer transaction we need to update the account_id
      transferValues.put(KEY_ACCOUNTID, getTransferAccountId());
      //the account from which is transfered could also have been altered
      transferValues.put(KEY_TRANSFER_ACCOUNT, getAccountId());
      Preconditions.checkNotNull(getUuid());
      transferValues.put(KEY_UUID, getUuid());
      ops.add(ContentProviderOperation
          .newUpdate(transferUri)
          .withValues(transferValues).build());
    }
    addOriginPlanInstance(ops);
    return ops;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    Transfer other = (Transfer) obj;
    if (getTransferAccountId() == null) {
      if (other.getTransferAccountId() != null)
        return false;
    } else if (!getTransferAccountId().equals(other.getTransferAccountId()))
      return false;
    if (getTransferPeer() == null) {
      if (other.getTransferPeer() != null)
        return false;
    } else if (!getTransferPeer().equals(other.getTransferPeer()))
      return false;
    return true;
  }

  @Override
  protected void updateFromResult(ContentProviderResult[] result) {
    super.updateFromResult(result);
    setTransferPeer(ContentUris.parseId(result[1].uri));
  }

  public boolean isSameCurrency() {
    return getAmount().getCurrencyUnit().equals(getTransferAmount().getCurrencyUnit());
  }

  public static String getIndicatorPrefixForLabel(long amount) {
    return ((amount < 0) ? RIGHT_ARROW : LEFT_ARROW) + " ";
  }

  public String printLabelWithPrefix() {
    return getIndicatorPrefixForLabel(getAmount().getAmountMinor()) + " " + getLabel();
  }

  @Override
  public int operationType() {
    return TYPE_TRANSFER;
  }
}
