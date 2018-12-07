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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.R;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class SelectFromTableDialogFragment extends CommitSafeDialogFragment implements OnClickListener {

  protected final boolean withNullItem;
  protected BriteContentResolver briteContentResolver;
  private Disposable itemDisposable;
  private ArrayAdapter<DataHolder> adapter;

  public SelectFromTableDialogFragment(boolean withNullItem) {
    this.withNullItem = withNullItem;
  }

  abstract int getDialogTitle();

  abstract Uri getUri();

  abstract String getColumn();

  abstract boolean onResult(ArrayList<String> labelList, long[] itemIds);

  abstract String[] getSelectionArgs();

  abstract String getSelection();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    briteContentResolver = new SqlBrite.Builder().build().wrapContentProvider(getContext().getContentResolver(), Schedulers.io());
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (itemDisposable != null && !itemDisposable.isDisposed()) {
      itemDisposable.dispose();
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    adapter = new ArrayAdapter<DataHolder>(getContext(), android.R.layout.simple_list_item_multiple_choice) {
      @Override
      public boolean hasStableIds() {
        return true;
      }

      @Override
      public long getItemId(int position) {
        return getItem(position).id;
      }
    };
    if (withNullItem) {
      adapter.add(new DataHolder(-1, getString(R.string.unmapped)));
    }
    itemDisposable = briteContentResolver.createQuery(getUri(),
        null, getSelection(), getSelectionArgs(), null, false)
        .mapToList((Cursor cursor) -> DataHolder.fromCursor(cursor, getColumn()))
        .subscribe(adapter::addAll);

    final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
        .setTitle(getDialogTitle())
        .setAdapter(adapter, null)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, null)
        .create();
    alertDialog.getListView().setItemsCanFocus(false);
    alertDialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    //prevent automatic dismiss on button click
    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> onClick(alertDialog, AlertDialog.BUTTON_POSITIVE));

      }
    });
    return alertDialog;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity() == null) {
      return;
    }
    ListView listView = ((AlertDialog) dialog).getListView();
    SparseBooleanArray positions = listView.getCheckedItemPositions();

    long[] itemIds = listView.getCheckedItemIds();
    boolean shouldDismiss = true;
    if (itemIds.length > 0) {
      ArrayList<String> labelList = new ArrayList<>();
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          labelList.add(adapter.getItem(positions.keyAt(i)).label);
        }
      }
      shouldDismiss = onResult(labelList, itemIds);
    }
    if (shouldDismiss) {
      dismiss();
    }
  }

  static class DataHolder {
    long id;
    String label;

    DataHolder(long id, String label) {
      this.id = id;
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }

    static DataHolder fromCursor(Cursor cursor, String labelColumn) {
      return new DataHolder(cursor.getLong(cursor.getColumnIndex(KEY_ROWID)),
          cursor.getString(cursor.getColumnIndex(labelColumn)));
    }
  }
}