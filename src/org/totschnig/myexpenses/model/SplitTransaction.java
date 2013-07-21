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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITED;

import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;

public class SplitTransaction extends Transaction {
  
  public SplitTransaction(long accountId,long amount) {
    super(accountId,amount);
    status = STATUS_UNCOMMITED;
    catId = SPLIT_CATID;
  }
  public SplitTransaction(long accountId, Money amount) {
    super(accountId,amount);
    status = STATUS_UNCOMMITED;
    catId = SPLIT_CATID;
  }
  /**
   * existing parts are deleted and and the uncommited ones are commited
   */
  public void commit() {
    String idStr = String.valueOf(id);
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_STATUS, 0);
    cr().update(CONTENT_URI,initialValues,KEY_ROWID + "= ? OR " + KEY_PARENTID + "= ?",
        new String[] {idStr,idStr});
  }
  /**
   * all Split Parts are cloned and we work with the uncommited clones
   */
  public void prepareForEdit() {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_STATUS, STATUS_UNCOMMITED);
    cr().update(CONTENT_URI,initialValues,KEY_PARENTID + "= ?",
        new String[] {String.valueOf(id)});
/*    cr().insert(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).appendPath("cloneSplitParts").build(),
        null);*/
  }
}
