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

package org.totschnig.myexpenses.dialog.select;

import android.os.Bundle;

import org.jetbrains.annotations.Nullable;
import org.totschnig.myexpenses.model.AggregateAccount;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES;

public abstract class SelectFromMappedTableDialogFragment extends SelectFilterDialog {

  protected SelectFromMappedTableDialogFragment(boolean withNullItem) {
    super(withNullItem);
  }

  @NonNull
  @Override
  String getColumn() {
    return KEY_LABEL;
  }

  @Override
  protected String getSelection() {
    return accountSelection(getArguments().getLong(KEY_ROWID));
  }

  @Override
  protected String[] getSelectionArgs() {
    return accountSelectionArgs(getArguments().getLong(KEY_ROWID));
  }

  protected void setArguments(long rowId) {
    Bundle args = new Bundle(1);
    args.putLong(KEY_ROWID, rowId);
    setArguments(args);
  }

  @Nullable
  public static String accountSelection(long accountId) {
    if (accountId > 0) {
      return KEY_ACCOUNTID + " = ?";
    } else if (accountId != AggregateAccount.HOME_AGGREGATE_ID) {
      return KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY +
          " = (SELECT " + KEY_CODE + " FROM " + TABLE_CURRENCIES + " WHERE " + KEY_ROWID + " = ?))";
    }
    return null;
  }

  @Nullable
  public static String[] accountSelectionArgs(long accountId) {
    return accountId == AggregateAccount.HOME_AGGREGATE_ID ? null : new String[]{String.valueOf(Math.abs(accountId))};
  }
}