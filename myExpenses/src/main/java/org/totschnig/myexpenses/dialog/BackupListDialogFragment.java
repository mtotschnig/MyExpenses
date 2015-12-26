package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class BackupListDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener,DialogUtils.CalendarRestoreStrategyChangedListener {
  RadioGroup mRestorePlanStrategie;
  Spinner selectBackupSpinner;
  RadioGroup.OnCheckedChangeListener mCalendarRestoreButtonCheckedChangeListener;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final String[] backupFiles = (String[]) getArguments().getSerializable("backupFiles");
    LayoutInflater li = LayoutInflater.from(getActivity());
    View view = li.inflate(R.layout.backup_restore_fallback_dialog, null);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
        android.R.layout.simple_spinner_item, backupFiles);
    selectBackupSpinner = ((Spinner) view.findViewById(R.id.select_backup));
    selectBackupSpinner.setAdapter(adapter);
    adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view);
    if (mRestorePlanStrategie != null) {
      mCalendarRestoreButtonCheckedChangeListener =
          DialogUtils.buildCalendarRestoreStrategyChangedListener(
              (ProtectedFragmentActivity) getActivity(), this);
      mRestorePlanStrategie.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    }
    return new AlertDialog.Builder(getActivity())
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
    if (getActivity()==null) {
      return;
    }
    final String[] backupFiles = (String[]) getArguments().getSerializable("backupFiles");
    if (which == AlertDialog.BUTTON_POSITIVE) {
      int position = selectBackupSpinner.getSelectedItemPosition();
      if (position!= AdapterView.INVALID_POSITION) {
        ((BackupRestoreActivity) getActivity()).onSourceSelected(
            backupFiles[position],
            mRestorePlanStrategie == null ? R.id.restore_calendar_handling_ignore :
                mRestorePlanStrategie.getCheckedRadioButtonId());
        return;
      }
    }
    onCancel(dialog);
  }

  @Override
  public void onResume() {
    super.onResume();
    setButtonState();
  }

  private void setButtonState() {
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
        mRestorePlanStrategie == null || mRestorePlanStrategie.getCheckedRadioButtonId() != -1);
  }

  @Override
  public void onCheckedChanged() {
    setButtonState();
  }

  @Override
  public void onCalendarPermissionDenied() {
    mRestorePlanStrategie.setOnCheckedChangeListener(null);
    mRestorePlanStrategie.clearCheck();
    mRestorePlanStrategie.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    setButtonState();
  }
}
