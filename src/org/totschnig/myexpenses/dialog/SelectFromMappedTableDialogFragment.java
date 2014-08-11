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

import org.totschnig.myexpenses.activity.MyExpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES;

import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.TextView;

public abstract class SelectFromMappedTableDialogFragment extends CommitSafeDialogFragment implements OnClickListener,
    LoaderManager.LoaderCallbacks<Cursor>
{
  protected SimpleCursorAdapter mAdapter;
  protected Cursor mCursor;
  
  abstract int getDialogTitle();
  abstract Criteria makeCriteria(long id, String label);
  abstract int getCommand();
  abstract Uri getUri();
  
  /**
   * needed by PaymentMethod to translate labels of default methods
   * @param label
   * @return
   */
  protected String getDisplayLabel(String label) {
    return label;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext1(getActivity());
    mAdapter = new SimpleCursorAdapter(wrappedCtx, android.R.layout.simple_list_item_single_choice, null,
        new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0) {
      @Override
      public void setViewText(TextView v, String text) {
        super.setViewText(v, getDisplayLabel(text));
      }
    };
    getLoaderManager().initLoader(0, null, this);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(getDialogTitle())
      .setSingleChoiceItems(mAdapter, -1, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null || mCursor == null) {
      return;
    }
    mCursor.moveToPosition(which);
    ((MyExpenses) getActivity()).addFilterCriteria(
        getCommand(),
        makeCriteria(
            mCursor.getLong(mCursor.getColumnIndex(KEY_ROWID)),
            getDisplayLabel(mCursor.getString(mCursor.getColumnIndex(KEY_LABEL)))));
    dismiss();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    if (getActivity()==null) {
      return null;
    }
    String selection,selectionArg;
    long accountId = getArguments().getLong(KEY_ACCOUNTID);
    if (accountId < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY +
          " = (SELECT " + KEY_CODE + " FROM " + TABLE_CURRENCIES + " WHERE " + KEY_ROWID + " = ?))";
      selectionArg = String.valueOf(Math.abs(accountId));
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArg = String.valueOf(accountId);
    }
    CursorLoader cursorLoader = new CursorLoader(
        getActivity(),
        getUri(),
        null,
        selection,
        new String[] {selectionArg},
        null);
    return cursorLoader;

  }
  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
    mCursor = data;
    mAdapter.swapCursor(data);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mCursor = null;
    mAdapter.swapCursor(null);
  }
}