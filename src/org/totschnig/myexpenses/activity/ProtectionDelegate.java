package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.DialogUtils;

import android.app.Activity;
import android.app.AlertDialog;

public class ProtectionDelegate {
  Activity ctx;
  public ProtectionDelegate(Activity ctx) {
    this.ctx = ctx;
  }
  protected void handleOnPause(AlertDialog pwDialog) {
    MyApplication app = MyApplication.getInstance();
    if (app.isLocked && pwDialog != null)
      pwDialog.dismiss();
    else {
      app.setmLastPause();
    }
  }
  protected void handleOnDestroy() {
    MyApplication.getInstance().setmLastPause();
  }
  protected void hanldeOnResume(AlertDialog pwDialog) {
    MyApplication app = MyApplication.getInstance();
    if (app.shouldLock()) {
      if (pwDialog == null)
        pwDialog = DialogUtils.passwordDialog(ctx);
      DialogUtils.showPasswordDialog(ctx,pwDialog);
    }
  }

}
