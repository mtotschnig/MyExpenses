package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class SimpleToastActivity extends ProtectedFragmentActivity {
  public static final String KEY_MESSAGE = "message_id";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      String message = getIntent().getStringExtra(KEY_MESSAGE);
      if (message != null) {
        showMessage(message);
        return;
      }
    }
    finish();
  }

  @Override
  public void reportMissingSnackbarContainer() { /*is expected*/ }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish();
  }
}
