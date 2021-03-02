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

package org.totschnig.myexpenses.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.SplitPartAdapter;
import org.totschnig.myexpenses.databinding.SplitPartsListBinding;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Account;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.activity.ConstantsKt.EDIT_REQUEST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;

public class SplitPartList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String KEY_PARENT_IS_TEMPLATE = "parentIsTemplate";
  private static final String KEY_ACCOUNT = "account";
  private static final int TRANSACTION_CURSOR = 5;
  private static final int SUM_CURSOR = 6;

  private SplitPartsListBinding binding;

  private SplitPartAdapter mAdapter;
  private long transactionSum = 0;
  private Money unsplitAmount;

  @State
  long parentId;

  @State
  long accountId;

  @Inject
  CurrencyFormatter currencyFormatter;

  public static @NonNull
  SplitPartList newInstance(Long transactionId, boolean isTemplate, Account account) {
    SplitPartList f = new SplitPartList();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_PARENTID, transactionId);
    bundle.putSerializable(KEY_ACCOUNT, account);
    bundle.putBoolean(KEY_PARENT_IS_TEMPLATE, isTemplate);
    f.setArguments(bundle);
    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    setRetainInstance(true);
    if (savedInstanceState == null) {
      parentId = requireArguments().getLong(KEY_PARENTID);
      accountId = requireArguments().getLong(KEY_ACCOUNTID);
    } else {
      Icepick.restoreInstanceState(this, savedInstanceState);
    }
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    binding = SplitPartsListBinding.inflate(inflater, container, false);

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN, KEY_AMOUNT};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category, R.id.amount};

    requireLoaders();
    // Now create a simple cursor adapter and set it to display
    final Account account = (Account) getArguments().getSerializable(KEY_ACCOUNT);
    mAdapter = new SplitPartAdapter(ctx, R.layout.split_part_row, null, from, to, 0,
        account.getCurrency(), currencyFormatter);
    binding.list.setAdapter(mAdapter);
    binding.list.setEmptyView(binding.empty);
    binding.list.setOnItemClickListener((a, v1, position, id) -> {
      Intent i = new Intent(ctx, ExpenseEdit.class);
      i.putExtra(parentIsTemplate() ? KEY_TEMPLATEID : KEY_ROWID, id);
      startActivityForResult(i, EDIT_REQUEST);
    });
    registerForContextMenu(binding.list);
    binding.CREATEPARTCOMMAND.setContentDescription(TextUtils.concatResStrings(getActivity(), ". ",
        R.string.menu_create_split_part_category, R.string.menu_create_split_part_transfer));
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                  ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (item.getItemId() == R.id.DELETE_COMMAND) {
      ((ProtectedFragmentActivity) getActivity()).startTaskExecution(
          parentIsTemplate() ? TaskExecutionFragment.TASK_DELETE_TEMPLATES : TaskExecutionFragment.TASK_DELETE_TRANSACTION,
          new Long[]{info.id},
          Boolean.FALSE,
          0);
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] selectionArgs = new String[]{String.valueOf(parentId)};
    CursorLoader cursorLoader = null;
    Uri uri = parentIsTemplate() ?
        TransactionProvider.TEMPLATES_UNCOMMITTED_URI : TransactionProvider.UNCOMMITTED_URI;
    switch (id) {
      case TRANSACTION_CURSOR:
        cursorLoader = new CursorLoader(getActivity(), uri, null, "parent_id = ?",
            selectionArgs, null);
        return cursorLoader;
      case SUM_CURSOR:
        cursorLoader = new CursorLoader(getActivity(), uri,
            new String[]{"sum(" + KEY_AMOUNT + ")"}, "parent_id = ?",
            selectionArgs, null);
    }
    return cursorLoader;
  }

  private boolean parentIsTemplate() {
    return getArguments().getBoolean(KEY_PARENT_IS_TEMPLATE);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    switch (arg0.getId()) {
      case TRANSACTION_CURSOR:
        mAdapter.swapCursor(c);
        break;
      case SUM_CURSOR:
        c.moveToFirst();
        transactionSum = c.getLong(0);
        updateBalance();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    if (arg0.getId() == TRANSACTION_CURSOR) {
      mAdapter.swapCursor(null);
    }
  }

  public void updateBalance() {
    ExpenseEdit ctx = (ExpenseEdit) getActivity();
    if (ctx == null)
      return;
    unsplitAmount = ctx.getAmount();
    //when we are called before transaction is loaded in parent activity
    if (unsplitAmount == null)
      return;
    unsplitAmount = new Money(unsplitAmount.getCurrencyUnit(), unsplitAmount.getAmountMinor() - transactionSum);
    binding.end.setText(currencyFormatter.formatCurrency(unsplitAmount));
  }

  public boolean splitComplete() {
    return unsplitAmount != null && unsplitAmount.getAmountMinor() == 0L;
  }

  public int getSplitCount() {
    return mAdapter.getCount();
  }

  public void updateAccount(Account account) {
    accountId = account.getId();
    mAdapter.setCurrency(account.getCurrency());
    mAdapter.notifyDataSetChanged();
    updateBalance();
  }

  public void updateParent(long parentId) {
    this.parentId = parentId;
    requireLoaders();
  }

  private void requireLoaders() {
    Utils.requireLoader(LoaderManager.getInstance(getActivity()), TRANSACTION_CURSOR, null, this);
    Utils.requireLoader(LoaderManager.getInstance(getActivity()), SUM_CURSOR, null, this);
  }
}
