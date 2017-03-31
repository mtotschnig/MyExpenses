package org.totschnig.myexpenses.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class SimpleToastActivity extends Activity {
  public static final String KEY_MESSAGE_ID = "message_id";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getIntent() != null) {
      int intExtra = getIntent().getIntExtra(KEY_MESSAGE_ID, 0);
      if (intExtra != 0) {
        Toast.makeText(this, intExtra, Toast.LENGTH_LONG).show();
      }
    }
    finish();
  }
}
