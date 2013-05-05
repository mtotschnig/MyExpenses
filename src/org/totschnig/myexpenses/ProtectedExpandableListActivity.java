package org.totschnig.myexpenses;

import android.app.Dialog;
import android.app.ExpandableListActivity;


public class ProtectedExpandableListActivity extends ExpandableListActivity {
  private Dialog pwDialog;
  @Override
  protected void onPause() {
    super.onPause();
    MyApplication app = MyApplication.getInstance();
    if (app.isLocked)
      pwDialog.dismiss();
    else {
      app.setmLastPause();
    }
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    MyApplication.getInstance().setmLastPause();
  }
  @Override
  protected void onResume() {
    super.onResume();
    MyApplication app = MyApplication.getInstance();
    if (app.shouldLock()) {
      if (pwDialog == null)
        pwDialog = DialogUtils.passwordDialog(this);
      DialogUtils.showPasswordDialog(this,pwDialog);
    }
  }
}
