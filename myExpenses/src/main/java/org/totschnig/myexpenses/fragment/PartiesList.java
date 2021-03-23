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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageParties;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.MenuUtilsKt;
import org.totschnig.myexpenses.viewmodel.PartyListViewModel;
import org.totschnig.myexpenses.viewmodel.data.Party;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import icepick.Icepick;
import icepick.State;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static org.totschnig.myexpenses.ConstantsKt.ACTION_MANAGE;
import static org.totschnig.myexpenses.ConstantsKt.ACTION_SELECT_FILTER;
import static org.totschnig.myexpenses.ConstantsKt.ACTION_SELECT_MAPPING;
import static org.totschnig.myexpenses.adapter.CategoryTreeBaseAdapter.NULL_ITEM_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.util.MenuUtilsKt.prepareSearch;

public class PartiesList extends ContextualActionBarFragment {
  public static final String DIALOG_EDIT_PARTY = "dialogEditParty";
  ArrayAdapter<Party> mAdapter;
  private PartyListViewModel viewModel;

  @State
  @Nullable
  String filter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    viewModel = new ViewModelProvider(this).get(PartyListViewModel.class);
    Icepick.restoreInstanceState(this, savedInstanceState);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  protected boolean withCommonContext() {
    return !(getAction().equals(ACTION_SELECT_FILTER));
  }

  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    if (super.dispatchCommandSingle(command, info)) {
      return true;
    }
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
    Party party = mAdapter.getItem(menuInfo.position);
    if (command == R.id.EDIT_COMMAND) {
      Bundle args = new Bundle();
      args.putLong(DatabaseConstants.KEY_ROWID, menuInfo.id);
      SimpleInputDialog.build()
          .title(R.string.menu_edit_party)
          .cancelable(false)
          .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
          .hint(R.string.label)
          .text(party.getName())
          .pos(R.string.menu_save)
          .neut()
          .extra(args)
          .show(this, DIALOG_EDIT_PARTY);
      return true;
    } else if (command == R.id.SELECT_COMMAND) {
      doSingleSelection(party);
      finishActionMode();
      return true;
    }
    return false;
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    if (super.dispatchCommandMultiple(command, positions, itemIds)) {
      return true;
    }
    ProtectedFragmentActivity activity = (ProtectedFragmentActivity) requireActivity();
    if (command == R.id.DELETE_COMMAND) {
      int hasMappedTransactionsCount = 0, hasMappedTemplatesCount = 0;
      ArrayList<Long> idList = new ArrayList<>();
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          boolean deletable = true;
          Party party = mAdapter.getItem(positions.keyAt(i));
          if (party.getMappedTransactions()) {
            hasMappedTransactionsCount++;
            deletable = false;
          }
          if (party.getMappedTemplates()) {
            hasMappedTemplatesCount++;
            deletable = false;
          }
          if (deletable) {
            idList.add(party.getId());
          }
        }
      }
      if (!idList.isEmpty()) {
        activity.startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_PAYEES,
            idList.toArray(new Long[0]),
            null,
            R.string.progress_dialog_deleting);
      }
      if (hasMappedTransactionsCount > 0 || hasMappedTemplatesCount > 0) {
        String message = "";
        if (hasMappedTransactionsCount > 0) {
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_transactions,
              hasMappedTransactionsCount,
              hasMappedTransactionsCount);
        }
        if (hasMappedTemplatesCount > 0) {
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_templates,
              hasMappedTemplatesCount,
              hasMappedTemplatesCount);
        }
        activity.showSnackbar(message);
      }
      return true;
    } else if (command == R.id.SELECT_COMMAND_MULTIPLE) {
      if (itemIds.length == 1 || !Arrays.asList(itemIds).contains(NULL_ITEM_ID)) {
        ArrayList<String> labelList = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
          if (positions.valueAt(i)) {
            Party party = mAdapter.getItem(positions.keyAt(i));
            labelList.add(party.getName());
          }
        }
        Intent intent = new Intent();
        intent.putExtra(KEY_PAYEEID, ArrayUtils.toPrimitive(itemIds));
        intent.putExtra(KEY_LABEL, TextUtils.join(",", labelList));
        activity.setResult(RESULT_FIRST_USER, intent);
        activity.finish();
      } else {
        activity.showSnackbar(R.string.unmapped_filter_only_single);
      }
      return true;
    }
    return false;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    if (getActivity() == null) return;
    inflater.inflate(R.menu.search, menu);
    MenuUtilsKt.configureSearch(getActivity(), menu, this::onQueryTextChange);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    prepareSearch(menu, filter);
  }

  private Boolean onQueryTextChange(String newText) {
    if (TextUtils.isEmpty(newText)) {
      filter = "";
    } else {
      filter = newText;
    }
    loadData();
    return true;
  }

  private void loadData() {
    viewModel.loadParties(filter, requireActivity().getIntent().getLongExtra(KEY_ACCOUNTID, 0));
  }

  private String getAction() {
    return ((ManageParties) getActivity()).getAction();
  }

  protected void doSingleSelection(Party party) {
    Activity ctx = getActivity();
    Intent intent = new Intent();
    intent.putExtra(KEY_PAYEEID, party.getId());
    intent.putExtra(KEY_LABEL, party.getName());
    ctx.setResult(RESULT_OK, intent);
    ctx.finish();
  }

  @Override
  @SuppressLint("InlinedApi")
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.parties_list, container, false);

    final ListView lv = v.findViewById(R.id.list);
    lv.setItemsCanFocus(false);
    String action = getAction();
    if (!action.equals(ACTION_MANAGE)) {
      lv.setOnItemClickListener((parent, view, position, id) -> doSingleSelection(mAdapter.getItem(position)));
    }

    mAdapter = new ArrayAdapter<Party>(requireContext(), android.R.layout.simple_list_item_activated_1) {
      @Override
      public boolean hasStableIds() {
        return true;
      }

      @Override
      public long getItemId(int position) {
        //BUG in Abslistview https://stackoverflow.com/a/15692815/1199911
        return position < getCount() ? getItem(position).getId() : -1;
      }
    };
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextualActionBar(lv);
    viewModel.getParties().observe(getViewLifecycleOwner(), parties -> {
      mAdapter.setNotifyOnChange(false);
      mAdapter.clear();
      if (getAction().equals(ACTION_SELECT_FILTER)) {
        mAdapter.add(new Party(NULL_ITEM_ID, getString(R.string.unmapped), false, false));
      }
      mAdapter.addAll(parties);
      mAdapter.notifyDataSetChanged();
    });
    loadData();
    return v;
  }

  @Override
  protected void inflateContextualActionBar(Menu menu, int listId) {
    super.inflateContextualActionBar(menu, listId);
    MenuInflater inflater = getActivity().getMenuInflater();
    if (hasSelectSingle()) {
      inflater.inflate(R.menu.select, menu);
    }
    if (hasSelectMultiple()) {
      inflater.inflate(R.menu.select_multiple, menu);
    }
  }

  protected boolean hasSelectSingle() {
    return getAction().equals(ACTION_SELECT_MAPPING);
  }

  protected boolean hasSelectMultiple() {
    return getAction().equals(ACTION_SELECT_FILTER);
  }
}
