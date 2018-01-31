package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class SimpleToastActivity extends ProtectedFragmentActivity {
  public static final String KEY_MESSAGE_ID = "message_id";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      int intExtra = getIntent().getIntExtra(KEY_MESSAGE_ID, 0);
      if (intExtra != 0) {
        showMessage(intExtra);
      }
    }
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish();
  }
}
