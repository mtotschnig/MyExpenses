package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;

public class SimpleToastActivity extends ProtectedFragmentActivity {
  public static final String KEY_MESSAGE_ID = "message_id";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      int intExtra = getIntent().getIntExtra(KEY_MESSAGE_ID, 0);
      if (intExtra != 0) {
        MessageDialogFragment.newInstance(
            0,
            R.string.dialog_command_disabled_insert_transfer,
            MessageDialogFragment.Button.okButton(),
            null, null)
            .show(getSupportFragmentManager(), "BUTTON_DISABLED_INFO");
      }
    }
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish();
  }
}
