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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.squareup.sqlbrite3.BriteContentResolver;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class SelectFromTableDialogFragment extends CommitSafeDialogFragment implements OnClickListener {

  private static final String KEY_CHECKED_POSITIONS = "checked_positions";
  private final boolean withNullItem;
  @Inject
  BriteContentResolver briteContentResolver;
  private Disposable itemDisposable;
  private SimpleCursorAdapter adapter;

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
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (itemDisposable != null && !itemDisposable.isDisposed()) {
      itemDisposable.dispose();
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_CHECKED_POSITIONS, new SparseBooleanArrayParcelable(((AlertDialog) getDialog()).getListView().getCheckedItemPositions()));
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final String[] projection = {KEY_ROWID, getColumn()};
    adapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice, null,
        new String[]{getColumn()}, new int[]{android.R.id.text1}, 0);
    itemDisposable = briteContentResolver.createQuery(getUri(),
        projection, getSelection(), getSelectionArgs(), null, false)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(query -> {
          final Activity activity = getActivity();
          if (activity != null) {
            Cursor cursor = query.run();
            if (withNullItem) {
              MatrixCursor extras = new MatrixCursor(projection);
              extras.addRow(new String[]{
                  "-1",
                  SelectFromTableDialogFragment.this.getString(R.string.unmapped),
              });
              cursor = new MergeCursor(new Cursor[]{extras, cursor});
            }
            adapter.swapCursor(cursor);
            if (savedInstanceState != null) {
              SparseBooleanArrayParcelable checkedItemPositions = savedInstanceState.getParcelable(KEY_CHECKED_POSITIONS);
              for (int i = 0; i < checkedItemPositions.size(); i++) {
                if (checkedItemPositions.valueAt(i)) {
                  ((AlertDialog) getDialog()).getListView().setItemChecked(checkedItemPositions.keyAt(i), true);
                }
              }
            }
          }
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
          final Cursor cursor = (Cursor) adapter.getItem(positions.keyAt(i));
          labelList.add(cursor.getString(cursor.getColumnIndex(getColumn())));
        }
      }
      shouldDismiss = onResult(labelList, itemIds, which);
    }
    if (shouldDismiss) {
      dismiss();
    }
  }
}