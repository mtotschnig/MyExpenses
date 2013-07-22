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
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;

public class SplitTransaction extends Transaction {
  
  public SplitTransaction(long accountId,long amount) {
    super(accountId,amount);
    status = STATUS_UNCOMMITTED;
    catId = SPLIT_CATID;
  }
  public SplitTransaction(long accountId, Money amount) {
    super(accountId,amount);
    status = STATUS_UNCOMMITTED;
    catId = SPLIT_CATID;
  }
  /**
   * existing parts are deleted and the uncommitted ones are committed
   */
  public void commit() {
    String idStr = String.valueOf(id);
    cr().delete(CONTENT_URI, KEY_PARENTID + "= ? AND " + KEY_STATUS + " != ?",
        new String[] { idStr, String.valueOf(STATUS_UNCOMMITTED) });
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_STATUS, 0);
    //for a new split, both the parent and the parts are in state uncommitted
    //when we edit a split only the parts are in state uncommitted,
    //in any case we only update the state for rows that are uncommitted, to
    //prevent altering the state of a parent (e.g. from exported to non-exported
    cr().update(CONTENT_URI,initialValues,"("+ KEY_ROWID + "= ? OR " + KEY_PARENTID + "= ? ) AND " + KEY_STATUS + " = ?",
        new String[] {idStr,idStr,String.valueOf(STATUS_UNCOMMITTED)});
  }
  /**
   * all Split Parts are cloned and we work with the uncommitted clones
   */
  public void prepareForEdit() {
    cr().bulkInsert(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).appendPath("cloneSplitParts").build(), null);
  }
  /**
   * delete all uncommitted rows in the database,
   * this assumes that there are never more than one edits in parallel
   */
  public void cleanupCanceledEdit() {
    cr().delete(CONTENT_URI, KEY_STATUS + " = ?",
        new String[] { String.valueOf(STATUS_UNCOMMITTED) });
  }
}
