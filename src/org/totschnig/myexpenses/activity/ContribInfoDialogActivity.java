package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
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
    protected long sequenceCount;
    public final static String KEY_FEATURE = "feature";
    public static final String KEY_TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Feature f = (Feature) getIntent().getSerializableExtra(KEY_FEATURE);

        if (f==null) {
          sequenceCount = getIntent().getLongExtra(ContribInfoDialogFragment.KEY_SEQUENCE_COUNT, -1);
          ContribInfoDialogFragment.newInstance(
              sequenceCount)
            .show(getSupportFragmentManager(),"CONTRIB_INFO");
        } else {
          ContribDialogFragment.newInstance(f,
              getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(),"CONTRIB");
        }
    }
    @Override
    public boolean dispatchCommand(int command, Object tag) {
      switch (command) {
      case R.id.REMIND_LATER_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(sequenceCount+MyExpenses.TRESHOLD_REMIND_CONTRIB);
        break;
      case R.id.REMIND_NO_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(-1);
      }
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
