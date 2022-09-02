package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.util.ImportFileResultHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class BackupSourcesDialogFragment extends ImportSourceDialogFragment
    implements DialogUtils.CalendarRestoreStrategyChangedListener {
  RadioGroup mRestorePlanStrategie;
  RadioGroup.OnCheckedChangeListener mCalendarRestoreButtonCheckedChangeListener;

  public BackupSourcesDialogFragment() {
    super();
  }

  //Normally, it is recommended to pass configuration to fragment via setArguments,
  //but since we safe uri in instance state, it is safe to set it in constructor
  public BackupSourcesDialogFragment(Uri data) {
    this();
    mUri = data;
  }

  public static BackupSourcesDialogFragment newInstance(Uri data) {
    return new BackupSourcesDialogFragment(data);
  }

  @Override
  protected int getLayoutId() {
    return R.layout.backup_restore_dialog;
  }

  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    final int selectorVisibility = ((BackupRestoreActivity) requireActivity()).getCalledExternally()
        ? View.GONE : View.VISIBLE;
    view.findViewById(R.id.summary).setVisibility(selectorVisibility);
    view.findViewById(R.id.btn_browse).setVisibility(selectorVisibility);
    mRestorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(view);
    if (mRestorePlanStrategie != null) {
      mCalendarRestoreButtonCheckedChangeListener =
          DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
      mRestorePlanStrategie.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Dialog dialog = super.onCreateDialog(savedInstanceState);
    if (savedInstanceState == null && mUri != null) {
      try {
        ImportFileResultHandler.handleFilenameRequestResult(this, mUri);
      } catch (Throwable throwable) {
        mUri = null;
        Toast.makeText(requireContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
        requireActivity().finish();
      }
    }
    return dialog;
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
      CrashHandler.report(new Exception(String.format(
          "Found resource with extension %s and unexpeceted mime type %s/%s",
          extension, typeParts[0], typeParts[1])));
      return true;
    }
    return false;
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity() == null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
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
