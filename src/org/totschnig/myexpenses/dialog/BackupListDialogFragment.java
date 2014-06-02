package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class BackupListDialogFragment extends DialogFragment {
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final String[] backupFiles = (String[]) getArguments().getSerializable("backupFiles");
    return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.pref_restore_title)
        .setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (getActivity()==null) {
              return;
            }
            ((BackupRestoreActivity) getActivity()).onSourceSelected(backupFiles[which]);
          }
        })
        .create();
  }

  public static BackupListDialogFragment newInstance(String[] backupFiles) {
    BackupListDialogFragment f = new BackupListDialogFragment();
    Bundle b = new Bundle();
    b.putSerializable("backupFiles", backupFiles);
    f.setArguments(b);
    return f;
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
}
