package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

  public class ContribInfoDialogActivity extends FragmentActivity implements MessageDialogListener,ContribIFace {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonCommands.showContribInfoDialog(this,false);
    }

    @Override
    public boolean dispatchCommand(int command, Object tag) {
        CommonCommands.dispatchCommand(this, command, tag);
        return true;
    }

    @Override
    public void onMessageDialogDismissOrCancel() {
     finish();
    }

    @Override
    public void contribFeatureCalled(Feature feature, Serializable tag) {
      // TODO Auto-generated method stub
    }

    @Override
    public void contribFeatureNotCalled() {
      finish();
    }
    @Override
    protected void onActivityResult(int arg0, int arg1, Intent arg2) {
      finish();
    }
}
