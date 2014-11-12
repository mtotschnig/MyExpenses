package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

  public class ContribInfoDialogActivity extends FragmentActivity
      implements MessageDialogListener,ContribIFace {

    public final static String KEY_FEATURE = "feature";
    public static final String KEY_TAG = "tag";
    public static final String KEY_REMINDER = "reminder";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Feature f = (Feature) getIntent().getSerializableExtra(KEY_FEATURE);
        if (f==null) {
          ContribInfoDialogFragment.newInstance(
              getIntent().getBooleanExtra(KEY_REMINDER, false))
            .show(getSupportFragmentManager(),"CONTRIB_INFO");
        } else {
          ContribDialogFragment.newInstance(f,
              getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(),"CONTRIB");
        }
    }
    @Override
    public boolean dispatchCommand(int command, Object tag) {
      CommonCommands.dispatchCommand(this, command, tag);
      finish();
      return true;
    }

    public void contribBuyDo() {
        DonateDialogFragment.newInstance().show(getSupportFragmentManager(),"CONTRIB");
    }

    @Override
    public void onMessageDialogDismissOrCancel() {
     finish();
    }

    @Override
    public void contribFeatureCalled(Feature feature, Serializable tag) {
      Intent i = new Intent();
      i.putExtra(KEY_FEATURE, feature);
      i.putExtra(KEY_TAG,tag);
      setResult(RESULT_OK, i);
      finish();
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
