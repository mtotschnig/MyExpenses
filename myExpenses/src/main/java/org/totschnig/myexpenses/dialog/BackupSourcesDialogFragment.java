package org.totschnig.myexpenses.dialog;

import android.content.DialogInterface;
import android.view.View;
import android.widget.RadioGroup;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;

import androidx.appcompat.app.AlertDialog;
import timber.log.Timber;

public class BackupSourcesDialogFragment extends ImportSourceDialogFragment
    implements DialogUtils.CalendarRestoreStrategyChangedListener {
  RadioGroup mRestorePlanStrategie;
  RadioGroup.OnCheckedChangeListener mCalendarRestoreButtonCheckedChangeListener;

  
  public static BackupSourcesDialogFragment newInstance() {
    return new BackupSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.backup_restore_dialog;
  }
  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view);
    if (mRestorePlanStrategie != null) {
      mCalendarRestoreButtonCheckedChangeListener =
          DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
      mRestorePlanStrategie.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    }
  }
  @Override
  protected String getLayoutTitle() {
    return getString(R.string.pref_restore_title);
  }

  @Override
  public String getTypeName() {
    return "Zip";
  }
  @Override
  public String getPrefKey() {
    return "backup_restore_file_uri";
  }

  @Override
  public boolean checkTypeParts(String[] typeParts, String extension) {
    if (typeParts[0].equals("application") &&
        (typeParts[1].equals("zip") || typeParts[1].equals("octet-stream")))
      return true;
    if (extension.equals("zip") || extension.equals("enc")) {
      Timber.e("Found resource with extension %s and unexpeceted mime type %s/%s", extension, typeParts[0], typeParts[1]);
      return true;
    }
    return false;
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
          mRestorePlanStrategie == null ? R.id.restore_calendar_handling_ignore :
              mRestorePlanStrategie.getCheckedRadioButtonId());
    } else {
      super.onClick(dialog, id);
    }
  }
  @Override
  protected boolean isReady() {
    if (super.isReady()) {
      return mRestorePlanStrategie == null || mRestorePlanStrategie.getCheckedRadioButtonId() != -1;
    } else {
      return false;
    }
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
