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
import android.widget.Button;
import android.widget.ListView;

import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.totschnig.myexpenses.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
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

  abstract boolean onResult(List<String> labelList, long[] itemIds, int which);

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
    itemDisposable = briteContentResolver.createQuery(getUri(),
        null, getSelection(), getSelectionArgs(), null, false)
        .mapToList((Cursor cursor) -> DataHolder.fromCursor(cursor, getColumn()))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(collection -> {
          adapter.clear();
          if (withNullItem) {
            adapter.add(new DataHolder(-1, getString(R.string.unmapped)));
          }
          adapter.addAll(collection);
          adapter.notifyDataSetChanged();
        });

    final int neutralButton = getNeutralButton();
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setTitle(getDialogTitle())
        .setAdapter(adapter, null)
        .setPositiveButton(getPositiveButton(), null)
        .setNegativeButton(getNegativeButton(), null);
    if (neutralButton != 0) {
        builder.setNeutralButton(neutralButton, null);
    }
    final AlertDialog alertDialog = builder.create();
    alertDialog.getListView().setItemsCanFocus(false);
    alertDialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    //prevent automatic dismiss on button click
    alertDialog.setOnShowListener(dialog -> {
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
          v -> onClick(alertDialog, AlertDialog.BUTTON_POSITIVE));
      Button neutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
      if (neutral != null) {
        neutral.setOnClickListener(v -> onClick(alertDialog, AlertDialog.BUTTON_NEUTRAL));
      }
    });
    return alertDialog;
  }

  protected int getNeutralButton() {
    return 0;
  }

  protected int getNegativeButton() {
    return android.R.string.cancel;
  }

  protected int getPositiveButton() {
    return android.R.string.ok;
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
      shouldDismiss = onResult(labelList, itemIds, which);
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