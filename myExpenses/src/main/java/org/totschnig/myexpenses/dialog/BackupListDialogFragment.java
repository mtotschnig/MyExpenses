package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class BackupListDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener,DialogUtils.CalendarRestoreStrategyChangedListener {
  RadioGroup mRestorePlanStrategie;
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final String[] backupFiles = (String[]) getArguments().getSerializable("backupFiles");
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.backup_restore_fallback_dialog, null);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(wrappedCtx,
        android.R.layout.simple_spinner_item, backupFiles);
    ((Spinner) view.findViewById(R.id.select_backup)).setAdapter(adapter);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view,this);
    return new AlertDialog.Builder(wrappedCtx)
        .setTitle(R.string.pref_restore_title)
        .setView(view)
        /*.setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (getActivity()==null) {
              return;
            }
            ((BackupRestoreActivity) getActivity()).onSourceSelected(backupFiles[which]);
          }
        })*/
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, this)
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

  @Override
  public void onClick(DialogInterface dialog, int which) {

  }

  @Override
  public void onResume() {
    super.onResume();
    setButtonState();
  }

  private void setButtonState() {
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
        mRestorePlanStrategie.getCheckedRadioButtonId() != -1);
  }

  @Override
  public void onCheckedChanged() {
    setButtonState();
  }
}
