package org.totschnig.myexpenses.dialog;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.RadioGroup;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

public class BackupSourcesDialogFragment extends ImportSourceDialogFragment
    implements DialogUtils.CalendarRestoreStrategyChangedListener {
  RadioGroup mRestorePlanStrategie;
  
  public static final BackupSourcesDialogFragment newInstance() {
    return new BackupSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.backup_restore_dialog;
  }
  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view,this);
  }
  @Override
  protected String getLayoutTitle() {
    return getString(R.string.pref_restore_title);
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
  public boolean checkTypeParts(String[] typeParts) {
    return typeParts[0].equals("application") && 
    typeParts[1].equals("zip");
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      maybePersistUri();
      ((BackupRestoreActivity) getActivity()).onSourceSelected(
          mUri,
          mRestorePlanStrategie.getCheckedRadioButtonId());
    } else {
      super.onClick(dialog, id);
    }
  }
  @Override
  protected boolean isReady() {
    if (super.isReady()) {
      return mRestorePlanStrategie.getCheckedRadioButtonId() != -1;
    } else {
      return false;
    }
  }

  @Override
  public void onCheckedChanged() {
    setButtonState();
  }
}
