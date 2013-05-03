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
      if (!(app.passwordHash.equals("")))
        app.setmLastPause(System.nanoTime());
    }
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    MyApplication app = MyApplication.getInstance();
    if (!(app.passwordHash.equals("")))
      app.setmLastPause(System.nanoTime());
  }
  @Override
  protected void onResume() {
    super.onResume();
    MyApplication app = MyApplication.getInstance();
    if (!app.passwordHash.equals("") && System.nanoTime() - app.getmLastPause() > 5000000000L) {
      pwDialog = Utils.passwordDialog(this);
      pwDialog.show();
    }
  }
}
