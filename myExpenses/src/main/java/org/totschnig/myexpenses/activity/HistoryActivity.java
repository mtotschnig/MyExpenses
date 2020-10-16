package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import org.totschnig.myexpenses.R;

public class HistoryActivity extends ProtectedFragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.history);
    setupToolbar(true);
  }
}
