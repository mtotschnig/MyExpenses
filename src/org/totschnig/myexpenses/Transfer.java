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

package org.totschnig.myexpenses;

import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * a transfer consists of a pair of transactions, one for each account
 * this class handles creation and update
 * @author Michael Totschnig
 *
 */
public class Transfer extends Transaction {
  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();
  
  public Transfer(long accountId,long amount) {
    super(accountId,amount);
  }
  public Transfer(long accountId, Money amount) {
    super(accountId,amount);
  }
  public static boolean delete(long id,long peer) {
    return MyApplication.cr().delete(TransactionProvider.TRANSACTIONS_URI,
        KEY_ROWID + " in (" + id + "," + peer + ")",null) > 0;
  }

  /* (non-Javadoc)
   * @see org.totschnig.myexpenses.Transaction#save()
   */
  public Uri save() {
    Uri uri;
    long amount = this.amount.getAmountMinor();
    //the id of the peer_account is stored in KEY_CATID,
    //the id of the peer transaction is stored in KEY_TRANSFER_PEER
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_DATE, dateAsString);
    initialValues.put(KEY_AMOUNT, amount);
    initialValues.put(KEY_CATID, catId);
    if (id == 0) {
      initialValues.put(KEY_ACCOUNTID, accountId);
      uri = MyApplication.cr().insert(TransactionProvider.TRANSACTIONS_URI, initialValues);
      long id = Integer.valueOf(uri.getLastPathSegment());
      initialValues.put(KEY_AMOUNT, 0 - amount);
      initialValues.put(KEY_CATID, accountId);
      initialValues.put(KEY_ACCOUNTID, catId);
      initialValues.put(KEY_TRANSFER_PEER,id);
      Uri transferUri = MyApplication.cr().insert(TransactionProvider.TRANSACTIONS_URI, initialValues);
      transfer_peer = Integer.valueOf(transferUri.getLastPathSegment());
      //we have to set the transfer_peer for the first transaction
      ContentValues args = new ContentValues();
      args.put(KEY_TRANSFER_PEER,transfer_peer);
      MyApplication.cr().update(Uri.parse(TransactionProvider.TRANSACTIONS_URI+ "/" + id), args, null, null);
    } else {
      uri = Uri.parse(TransactionProvider.TRANSACTIONS_URI + "/" + id);
      MyApplication.cr().update(uri,initialValues,null,null);
      initialValues.put(KEY_AMOUNT, 0 - amount);
      //if the user has changed the account to which we should transfer,
      //in the peer transaction we need to update the account_id
      initialValues.put(KEY_ACCOUNTID, catId);
      //the account from which is transfered is not altered
      initialValues.remove(KEY_CATID);
      MyApplication.cr().update(Uri.parse(TransactionProvider.TRANSACTIONS_URI + "/" + transfer_peer),initialValues,null,null);
    }
    return uri;
  }
}
