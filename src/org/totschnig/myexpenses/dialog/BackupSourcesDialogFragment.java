package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class BackupSourcesDialogFragment extends ImportSourceDialogFragment implements
DialogInterface.OnClickListener {
  
  public static final BackupSourcesDialogFragment newInstance() {
    return new BackupSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.backup_restore_dialog;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(new ButtonOnShowDisabler());
    return dialog;
  }
  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    ((RadioGroup) view.findViewById(R.id.restore_calendar_handling)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
      
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        Button b = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        b.setEnabled(checkedId!=-1);
        b.invalidate();
      }
    });
  }
  @Override
  protected int getLayoutTitle() {
    return R.string.pref_restore_title;
  }

  @Override
  String getTypeName() {
    return "Zip";
  }
  @Override
  String getPrefKey() {
    return "backup_restore_file_uri";
  }
  @Override
  protected String getMimeType() {
    return "application/zip";
  }
  @Override
  protected boolean checkTypeParts(String[] typeParts) {
    return typeParts[0].equals("application") && 
    typeParts[1].equals("zip");
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
        .putString(getPrefKey(), mUri.toString()));
      ((BackupRestoreActivity) getActivity()).onSourceSelected(mUri);
    } else {
      super.onClick(dialog, id);
    }
  }
}
