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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.IdCriteria;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


/**
 * uses {@link MessageDialogFragment.MessageDialogListener} to dispatch result back to activity
 *
 */
public class SelectPayerDialogFragment extends CommitSafeDialogFragment implements OnClickListener,
    LoaderManager.LoaderCallbacks<Cursor>
{
  /**
   * @param account_id
   * @return
   */
  public static final SelectPayerDialogFragment newInstance(long account_id) {
    SelectPayerDialogFragment dialogFragment = new SelectPayerDialogFragment();
    Bundle args = new Bundle();
    args.putLong(KEY_ACCOUNTID, account_id);
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  private SimpleCursorAdapter mPayeeAdapter;
  private Cursor mPayeeCursor;
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    mPayeeAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_single_choice, null,
        new String[] {KEY_PAYEE_NAME}, new int[] {android.R.id.text1}, 0);
    getLoaderManager().initLoader(0, null, this);
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.search_payee)
      .setSingleChoiceItems(mPayeeAdapter, -1, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null || mPayeeCursor == null) {
      return;
    }
    mPayeeCursor.moveToPosition(which);
    ((MyExpenses) getActivity()).addFilterCriteria(
        R.id.FILTER_STATUS_COMMAND,
        new IdCriteria(getString(R.string.payer_or_payee),
            KEY_PAYEEID,
            mPayeeCursor.getLong(mPayeeCursor.getColumnIndex(KEY_ROWID)),
            mPayeeCursor.getString(mPayeeCursor.getColumnIndex(KEY_PAYEE_NAME))));
    dismiss();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    if (getActivity()==null) {
      return null;
    }
    CursorLoader cursorLoader = new CursorLoader(
        getActivity(),
        TransactionProvider.PAYEES_FILTERED_URI,
        null,
        KEY_ACCOUNTID + " = ?",
        new String[] {String.valueOf(getArguments().getLong(KEY_ACCOUNTID))},
        null);
    return cursorLoader;

  }
  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
    mPayeeCursor = data;
    mPayeeAdapter.swapCursor(data);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mPayeeCursor = null;
    mPayeeAdapter.swapCursor(null);
  }
}