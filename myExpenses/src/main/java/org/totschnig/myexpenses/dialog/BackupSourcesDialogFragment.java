package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
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
  RadioGroup restorePlanStrategy;
  RadioGroup.OnCheckedChangeListener mCalendarRestoreButtonCheckedChangeListener;

  CheckBox encrypt;

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
    restorePlanStrategy = NewDialogUtilsKt.configureCalendarRestoreStrategy(view, prefHandler);
    mCalendarRestoreButtonCheckedChangeListener =
        DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
    restorePlanStrategy.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    encrypt = view.findViewById(R.id.encrypt_database);
    if (prefHandler.getEncryptDatabase()) {
      encrypt.setVisibility(View.VISIBLE);
      encrypt.setChecked(true);
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
          "Found resource with extension %s and unexpected mime type %s/%s",
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
          restorePlanStrategy == null ? R.id.restore_calendar_handling_ignore :
              restorePlanStrategy.getCheckedRadioButtonId(),
              prefHandler.getEncryptDatabase() && encrypt.isChecked());
    } else {
      super.onClick(dialog, id);
    }
  }

  @Override
  protected boolean isReady() {
    if (super.isReady()) {
      return restorePlanStrategy == null || restorePlanStrategy.getCheckedRadioButtonId() != -1;
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
    restorePlanStrategy.setOnCheckedChangeListener(null);
    restorePlanStrategy.clearCheck();
    restorePlanStrategy.setOnCheckedChangeListener(mCalendarRestoreButtonCheckedChangeListener);
    setButtonState();
  }
}
