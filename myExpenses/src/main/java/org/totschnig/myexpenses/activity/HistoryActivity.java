package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

public class HistoryActivity extends ProtectedFragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.history);
    setupToolbar(true);
  }
}
