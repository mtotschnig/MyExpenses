package org.totschnig.myexpenses.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.MultiSpinner;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RestoreFromCloudDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener {
  private static final String KEY_BACKUP_LIST = "backupList";
  private static final String KEY_SYNC_ACCOUNT_LIST = "syncAccountList";
  @BindView(R.id.restore_from_backup_button)
  protected RadioButton fromBackupButton;
  @BindView(R.id.restore_from_sync_accounts_button)
  protected RadioButton fromSyncAccountButton;
  @BindView(R.id.backup_list)
  protected Spinner backupListSpinner;
  @BindView(R.id.sync_account_list)
  protected MultiSpinner syncAccountListSpinner;
  @BindView(R.id.restore_from_backup_container)
  protected ViewGroup backupListContainer;
  @BindView(R.id.restore_from_sync_account_container)
  protected ViewGroup syncAccountListContainer;


  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    @SuppressLint("InflateParams")
    final View view = LayoutInflater.from(ctx).inflate(R.layout.restore_from_cloud, null);
    ButterKnife.bind(this, view);
    ArrayList<String> backups = getArguments().getStringArrayList(KEY_BACKUP_LIST);
    ArrayList<String> syncAccounts = getArguments().getStringArrayList(KEY_SYNC_ACCOUNT_LIST);
    if (backups != null && backups.size() > 0) {
      backupListSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_spinner_item, backups));
    } else {
      backupListContainer.setVisibility(View.GONE);
    }
    if (syncAccounts != null && syncAccounts.size() > 0) {
      syncAccountListSpinner.setItems(syncAccounts, "TODO", null);
    } else {
      syncAccountListContainer.setVisibility(View.GONE);
    }

    final AlertDialog dialog = new AlertDialog.Builder(ctx)
        .setTitle(R.string.onboarding_restore_from_cloud)
        .setView(view)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel,null)
        .create();
    dialog.setOnShowListener(new ButtonOnShowDisabler());
    return dialog;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {

  }

  @OnClick({ R.id.restore_from_backup_button })
  public void handleFromBackupCheck(RadioButton button) {
    fromSyncAccountButton.setChecked(false);
    enableSubmit();
  }

  @OnClick({ R.id.restore_from_sync_accounts_button })
  public void handleFromSynchAccountCheck(RadioButton button) {
    fromBackupButton.setChecked(false);
    enableSubmit();
  }

  private void enableSubmit() {
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
  }

  public static RestoreFromCloudDialogFragment newInstance(List<String> backupList, List<String> syncAccountList) {
    Bundle arguments = new Bundle(2);
    arguments.putStringArrayList(KEY_BACKUP_LIST, new ArrayList<>(backupList));
    arguments.putStringArrayList(KEY_SYNC_ACCOUNT_LIST, new ArrayList<>(syncAccountList));
    RestoreFromCloudDialogFragment fragment = new RestoreFromCloudDialogFragment();
    fragment.setArguments(arguments);
    return fragment;
  }
}
