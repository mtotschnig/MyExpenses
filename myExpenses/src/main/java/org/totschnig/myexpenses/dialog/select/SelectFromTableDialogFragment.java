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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.sqlbrite3.BriteContentResolver;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BaseDialogFragment;
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static android.widget.AbsListView.CHOICE_MODE_MULTIPLE;
import static android.widget.AbsListView.CHOICE_MODE_NONE;
import static android.widget.AdapterView.INVALID_ROW_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public abstract class SelectFromTableDialogFragment extends BaseDialogFragment implements OnClickListener {

  private static final String KEY_CHECKED_POSITIONS = "checked_positions";
  public static final String KEY_DIALOG_TITLE = "dialog_tile";
  public static final String KEY_EMPTY_MESSAGE = "empty_message";
  protected static final long NULL_ITEM_ID = 0L;
  protected static final long EMPTY_ITEM_ID = -1L;
  private final boolean withNullItem;
  @Inject
  BriteContentResolver briteContentResolver;
  private Disposable itemDisposable;
  protected ArrayAdapter<DataHolder> adapter;

  public SelectFromTableDialogFragment(boolean withNullItem) {
    this.withNullItem = withNullItem;
  }

  protected int getDialogTitle() {
    return 0;
  }

  @NonNull
  abstract Uri getUri();

  @NonNull
  abstract String getColumn();

  @Nullable
  protected String[] getSelectionArgs() {
    return null;
  }

  @Nullable
  protected String getSelection() {
    return null;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
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
    final int layout = getChoiceMode() == CHOICE_MODE_MULTIPLE ? android.R.layout.simple_list_item_multiple_choice : android.R.layout.simple_list_item_single_choice;
    adapter = new ArrayAdapter<DataHolder>(getContext(), layout) {
      @Override
      public boolean hasStableIds() {
        return true;
      }

      @Nullable
      @Override
      public DataHolder getItem(int position) {
        //workaround for framework bug, which causes getItem to be called upon orientation change with invalid position
        if (getCount() == 0) return null;
        return super.getItem(position);
      }

      @NonNull
      @Override
      public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final DataHolder item = getItem(position);
        if (item.getId() != EMPTY_ITEM_ID) return super.getView(position, convertView, parent);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        @SuppressLint("ViewHolder")
        TextView textView = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        textView.setText(item.getLabel());
        return textView;
      }

      @Override
      public long getItemId(int position) {
        final DataHolder item = getItem(position);
        return item != null ? item.getId() : INVALID_ROW_ID;
      }
    };
    itemDisposable = briteContentResolver.createQuery(getUri(),
        projection, getSelection(), getSelectionArgs(), null, false)
        .mapToList((Cursor cursor) -> DataHolder.fromCursor(cursor, getColumn()))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(collection -> {
          adapter.clear();
          final Activity activity = getActivity();
          if (activity != null) {
            final AlertDialog alertDialog = (AlertDialog) getDialog();
            if (withNullItem) {
              adapter.add(new DataHolder(NULL_ITEM_ID, getString(R.string.unmapped)));
            } else if (collection.size() == 0) {
              Button neutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
              if (neutral != null) {
                neutral.setVisibility(View.GONE);
              }
              Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
              if (positive != null) {
                positive.setVisibility(View.GONE);
              }
              adapter.add(new DataHolder(EMPTY_ITEM_ID, getEmptyMessage()));
              alertDialog.getListView().setChoiceMode(CHOICE_MODE_NONE);
            }
            adapter.addAll(collection);
            adapter.notifyDataSetChanged();
            if (savedInstanceState != null) {
              SparseBooleanArrayParcelable checkedItemPositions = savedInstanceState.getParcelable(KEY_CHECKED_POSITIONS);
              for (int i = 0; i < checkedItemPositions.size(); i++) {
                if (checkedItemPositions.valueAt(i)) {
                  alertDialog.getListView().setItemChecked(checkedItemPositions.keyAt(i), true);
                }
              }
            }
          }
        });

    final int neutralButton = getNeutralButton();
    final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity())
        .setAdapter(adapter, null)
        .setPositiveButton(getPositiveButton(), null)
        .setNegativeButton(getNegativeButton(), null);
    int dialogTitle = getDialogTitle();
    if (dialogTitle != 0) {
      builder.setTitle(dialogTitle);
    }
    if (neutralButton != 0) {
      builder.setNeutralButton(neutralButton, null);
    }
    final AlertDialog alertDialog = builder.create();
    alertDialog.getListView().setItemsCanFocus(false);
    alertDialog.getListView().setChoiceMode(getChoiceMode());
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

  @NonNull
  private String getEmptyMessage() {
    final Bundle arguments = getArguments();
    int resId = 0;
    if (arguments != null) {
      resId = arguments.getInt(KEY_EMPTY_MESSAGE);
      if (resId != 0) return getString(resId);
    }
    return "No data";
  }

  protected int getChoiceMode() {
    return CHOICE_MODE_MULTIPLE;
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
  public abstract void onClick(DialogInterface dialog, int which);

}