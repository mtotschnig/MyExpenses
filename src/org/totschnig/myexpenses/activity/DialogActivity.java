package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

  public class DialogActivity extends FragmentActivity implements MessageDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonCommands.showContribInfoDialog(this,false);
    }

    @Override
    public boolean dispatchCommand(int command, Object tag) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void onMessageDialogDismissOrCancel() {
     finish();
    }
}
