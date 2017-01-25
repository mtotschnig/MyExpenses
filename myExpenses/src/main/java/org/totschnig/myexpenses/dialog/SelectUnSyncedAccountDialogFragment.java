package org.totschnig.myexpenses.dialog;

import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_SAVE;

public class SelectUnSyncedAccountDialogFragment extends SelectFromTableDialogFragment {

  public static SelectUnSyncedAccountDialogFragment newInstance(String accountName) {
    SelectUnSyncedAccountDialogFragment dialogFragment = new SelectUnSyncedAccountDialogFragment();
    Bundle args = new Bundle();
    args.putString(KEY_SYNC_ACCOUNT_NAME, accountName);
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @Override
  int getDialogTitle() {
    return R.string.select_unsynced_accounts;
  }

  @Override
  Uri getUri() {
    return TransactionProvider.ACCOUNTS_URI;
  }

  @Override
  String getColumn() {
    return DatabaseConstants.KEY_LABEL;
  }

  @Override
  void onResult(ArrayList<String> labelList, long[] itemIds) {
    if (itemIds.length > 0) {
      ((ProtectedFragmentActivity) getActivity()).startTaskExecution(TASK_SYNC_LINK_SAVE,
          ArrayUtils.toObject(itemIds), getArguments().getString(KEY_SYNC_ACCOUNT_NAME), 0);
    }
  }

  @Override
  String[] getSelectionArgs() {
    return null;
  }

  @Override
  String getSelection() {
    return KEY_SYNC_ACCOUNT_NAME + " IS NULL";
  }
}
