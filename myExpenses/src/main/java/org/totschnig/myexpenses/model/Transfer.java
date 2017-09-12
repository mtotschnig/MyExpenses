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

import java.util.ArrayList;

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

/**
 * a transfer consists of a pair of transactions, one for each account
 * this class handles creation and update
 *
 * @author Michael Totschnig
 */
public class Transfer extends Transaction {

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

  public Transfer(Long accountId, Long amount) {
   this(accountId, amount, null);
  }

  public Transfer(Long accountId, Long amount, Long transferAccountId) {
    super(accountId, amount);
    this.setTransferAccountId(transferAccountId);
  }

  public Transfer(Long accountId, Money amount) {
    this(accountId, amount, null);
  }

  public Transfer(Long accountId, Money amount, Long transferAccountId) {
    super(accountId, amount);
    this.setTransferAccountId(transferAccountId);
  }

  public Transfer(Account account, long amount) {
    super(account, amount);
  }

  public Transfer(Account account, long amount, Account transferAccount) {
    super(account, amount);
    this.setTransferAccountId(transferAccount.getId());
  }

  @Override
  public void setAmount(Money amount) {
    if (getTransferAmount() != null && !amount.getCurrency().getCurrencyCode().equals(getTransferAmount().getCurrency().getCurrencyCode())) {
      throw new UnsupportedOperationException("for foreign exchange transfers, use setAmountAndTransferAmount");
    }
    super.setAmount(amount);
    this.setTransferAmount(new Money(amount.getCurrency(), amount.getAmountMajor().negate()));
  }

  public void setAmountAndTransferAmount(Money amount, Money transferAmount) {
    super.setAmount(amount);
    this.setTransferAmount(transferAmount);
  }

  /**
   * @param accountId         if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
   * @param transferAccountId
   * @return
   */
  public static Transfer getNewInstance(long accountId, Long transferAccountId) {
    Account account = Account.getInstanceFromDbWithFallback(accountId);
    if (account == null) {
      return null;
    }
    Account transferAccount = Account.getInstanceFromDbWithFallback(transferAccountId);
    if (transferAccount == null) {
      return null;
    }
    return new Transfer(account, 0L, transferAccount);
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset, boolean callerIsSyncAdapter) {
    Uri uri = getUriForSave(callerIsSyncAdapter);
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    long amount = this.getAmount().getAmountMinor();
    long transferAmount = this.getTransferAmount().getAmountMinor();
    //the id of the peer_account is stored in KEY_TRANSFER_ACCOUNT,
    //the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, getComment());
    initialValues.put(KEY_DATE, getDate().getTime() / 1000);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_TRANSFER_ACCOUNT, getTransferAccountId());
    initialValues.put(KEY_CR_STATUS, crStatus.name());
    initialValues.put(KEY_ACCOUNTID, getAccountId());
    savePicture(initialValues);
    if (getId() == 0) {
      //both parts of the transfer share uuid
      initialValues.put(KEY_UUID, requireUuid());
      initialValues.put(KEY_PARENTID, getParentId());
      initialValues.put(KEY_STATUS, status);
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri).withValues(initialValues);
      if (parentOffset != -1) {
        builder.withValueBackReference(KEY_PARENTID, parentOffset);
      }
      ops.add(builder.build());
      //if the transfer is part of a split, the transfer peer needs to have a null parent
      ContentValues transferValues = new ContentValues(initialValues);
      transferValues.remove(KEY_PARENTID);
      transferValues.put(KEY_AMOUNT, transferAmount);
      transferValues.put(KEY_TRANSFER_ACCOUNT, getAccountId());
      transferValues.put(KEY_ACCOUNTID, getTransferAccountId());
      ops.add(ContentProviderOperation.newInsert(uri)
          .withValues(transferValues).withValueBackReference(KEY_TRANSFER_PEER, offset)
          .build());
      //we have to set the transferPeer for the first transaction
      ContentValues args = new ContentValues();
      args.put(KEY_TRANSFER_PEER, getTransferPeer());
      ops.add(ContentProviderOperation.newUpdate(uri)
          .withValueBackReference(KEY_TRANSFER_PEER, offset + 1)
          .withSelection(KEY_ROWID + " = ?", new String[]{""})//replaced by back reference
          .withSelectionBackReference(0, offset)
          .build());
      addOriginPlanInstance(ops);
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
      transferValues.put(KEY_AMOUNT, transferAmount);
      //if the user has changed the account to which we should transfer,
      //in the peer transaction we need to update the account_id
      transferValues.put(KEY_ACCOUNTID, getTransferAccountId());
      //the account from which is transfered could also have been altered
      transferValues.put(KEY_TRANSFER_ACCOUNT, getAccountId());
      transferValues.put(KEY_UUID, uuid);
      ops.add(ContentProviderOperation
          .newUpdate(transferUri)
          .withValues(transferValues).build());
    }
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
    return getAmount().getCurrency().equals(getTransferAmount().getCurrency());
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
