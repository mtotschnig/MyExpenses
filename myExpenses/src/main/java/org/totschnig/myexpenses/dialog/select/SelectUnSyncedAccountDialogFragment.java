package org.totschnig.myexpenses.dialog.select;

import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.List;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_SAVE;

public class SelectUnSyncedAccountDialogFragment extends SelectMultipleDialogFragment {

  public SelectUnSyncedAccountDialogFragment() {
    super(false);
  }

  public static SelectUnSyncedAccountDialogFragment newInstance(String accountName) {
    SelectUnSyncedAccountDialogFragment dialogFragment = new SelectUnSyncedAccountDialogFragment();
    Bundle args = new Bundle();
    args.putString(KEY_SYNC_ACCOUNT_NAME, accountName);
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @Override
  protected int getDialogTitle() {
    return R.string.select_unsynced_accounts;
  }

  @NonNull
  @Override
  Uri getUri() {
    return TransactionProvider.ACCOUNTS_URI;
  }

  @NonNull
  @Override
  String getColumn() {
    return DatabaseConstants.KEY_LABEL;
  }

  @Override
  protected boolean onResult(List<String> labelList, long[] itemIds, int which) {
    if (itemIds.length > 0) {
      ((ProtectedFragmentActivity) getActivity()).startTaskExecution(TASK_SYNC_LINK_SAVE,
          ArrayUtils.toObject(itemIds), getArguments().getString(KEY_SYNC_ACCOUNT_NAME), 0);
    }
    return true;
  }

  @Override
  protected String getSelection() {
    return KEY_SYNC_ACCOUNT_NAME + " IS NULL AND " + KEY_SEALED + " = 0";
  }
}
