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

import java.util.ArrayList;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * a transfer consists of a pair of transactions, one for each account
 * this class handles creation and update
 * @author Michael Totschnig
 *
 */
public class Transfer extends Transaction {
  
  public Transfer(long accountId, Long amount) {
    super(accountId,amount);
    this.transferAmount = new Money(this.amount.getCurrency(),this.amount.getAmountMajor().negate());
  }

  public Transfer(long accountId, Money amount) {
    super(accountId,amount);
    this.transferAmount = new Money(amount.getCurrency(),amount.getAmountMajor().negate());
  }

  public Transfer(Account account, long amount, Account transferAccount) {
    super(account,amount);
    this.transfer_account = transferAccount.getId();
    this.transferAmount = new Money(transferAccount.currency,this.amount.getAmountMajor().negate());
  }

  @Override
  public void setAmount(Money amount) {
    if (!amount.getCurrency().getCurrencyCode().equals(transferAmount.getCurrency().getCurrencyCode())) {
      throw new UnsupportedOperationException("for foreign exchange transfers, use setAmountAndTransferAmount");
    }
    super.setAmount(amount);
    this.transferAmount = new Money(amount.getCurrency(),amount.getAmountMajor().negate());
  }

  public void setAmountAndTransferAmount(Money amount, Money transferAmount) {
    this.amount = amount;
    this.transferAmount = transferAmount;
  }

  /**
   * @param accountId if account no longer exists {@link Account#getInstanceFromDb(long) is called with 0}
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
    return new Transfer(account,0L,transferAccount);
  }

  @Override
  public ArrayList<ContentProviderOperation> buildSaveOperations(int offset, int parentOffset) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    long amount = this.amount.getAmountMinor();
    long transferAmount = this.transferAmount.getAmountMinor();
    //the id of the peer_account is stored in KEY_TRANSFER_ACCOUNT,
    //the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, date.getTime()/1000);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_TRANSFER_ACCOUNT, transfer_account);
    initialValues.put(KEY_CR_STATUS,crStatus.name());
    initialValues.put(KEY_ACCOUNTID, accountId);
    savePicture(initialValues);
    if (getId() == 0) {
      //both parts of the transfer share uuid
      initialValues.put(KEY_UUID, generateUuid());
      initialValues.put(KEY_PARENTID, parentId);
      initialValues.put(KEY_STATUS, status);
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(CONTENT_URI).withValues(initialValues);
      if (parentOffset != -1) {
        builder.withValueBackReference(KEY_PARENTID, parentOffset);
      }
      ops.add(builder.build());
      //if the transfer is part of a split, the transfer peer needs to have a null parent
      ContentValues transferValues = new ContentValues(initialValues);
      transferValues.remove(KEY_PARENTID);
      transferValues.put(KEY_AMOUNT, transferAmount);
      transferValues.put(KEY_TRANSFER_ACCOUNT, accountId);
      transferValues.put(KEY_ACCOUNTID, transfer_account);
      ops.add(ContentProviderOperation.newInsert(CONTENT_URI)
          .withValues(transferValues).withValueBackReference(KEY_TRANSFER_PEER, offset)
          .build());
      //we have to set the transfer_peer for the first transaction
      ContentValues args = new ContentValues();
      args.put(KEY_TRANSFER_PEER,transfer_peer);
      ops.add(ContentProviderOperation.newUpdate(CONTENT_URI)
          .withValueBackReference(KEY_TRANSFER_PEER, offset + 1)
          .withSelection(KEY_ROWID + " = ?", new String[]{""})//replaced by back reference
          .withSelectionBackReference(0, offset)
          .build());
      addOriginPlanInstance(ops);
    } else {
      ops.add(ContentProviderOperation
          .newUpdate(CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build())
          .withValues(initialValues).build());
      ContentValues transferValues = new ContentValues(initialValues);
      transferValues.put(KEY_AMOUNT, transferAmount);
      //if the user has changed the account to which we should transfer,
      //in the peer transaction we need to update the account_id
      transferValues.put(KEY_ACCOUNTID, transfer_account);
      //the account from which is transfered could also have been altered
      transferValues.put(KEY_TRANSFER_ACCOUNT,accountId);
      ops.add(ContentProviderOperation
          .newUpdate(CONTENT_URI.buildUpon().appendPath(String.valueOf(transfer_peer)).build())
          .withValues(transferValues).build());
    }
    return ops;
  }

  @Override
  protected void updateFromResult(ContentProviderResult[] result) {
    super.updateFromResult(result);
    transfer_peer = ContentUris.parseId(result[1].uri);
  }

  public boolean isSameCurrency() {
    return amount.getCurrency().equals(transferAmount.getCurrency());
  }
}
