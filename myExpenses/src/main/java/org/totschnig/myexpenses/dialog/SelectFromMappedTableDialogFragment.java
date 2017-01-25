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

package org.totschnig.myexpenses.dialog;

import android.text.TextUtils;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.filter.Criteria;

import java.util.ArrayList;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES;

public abstract class SelectFromMappedTableDialogFragment extends SelectFromTableDialogFragment
{
  abstract Criteria makeCriteria(String label, long... id);
  abstract int getCommand();

  @Override
  String getColumn() {
    return KEY_LABEL;
  }

  @Override
  void onResult(ArrayList<String> labelList, long[] itemIds) {
    ((MyExpenses) getActivity()).addFilterCriteria(
        getCommand(),
        makeCriteria(TextUtils.join(",", labelList), itemIds));
  }

  @Override
  String getSelection() {
    if (getArguments().getLong(KEY_ACCOUNTID) < 0) {
      return KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY +
          " = (SELECT " + KEY_CODE + " FROM " + TABLE_CURRENCIES + " WHERE " + KEY_ROWID + " = ?))";
    } else {
      return KEY_ACCOUNTID + " = ?";
    }
  }

  @Override
  String[] getSelectionArgs() {
    return new String[]{String.valueOf(Math.abs(getArguments().getLong(KEY_ACCOUNTID)))};
  }

}