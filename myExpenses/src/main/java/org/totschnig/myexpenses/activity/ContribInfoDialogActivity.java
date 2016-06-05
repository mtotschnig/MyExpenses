package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature;

import android.content.Intent;
import android.os.Bundle;

public class ContribInfoDialogActivity extends ProtectedFragmentActivity
    implements MessageDialogListener {
  protected long sequenceCount;
  public final static String KEY_FEATURE = "feature";
  public static final String KEY_TAG = "tag";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(MyApplication.getThemeIdTranslucent());
    ContribFeature f = (ContribFeature) getIntent().getSerializableExtra(KEY_FEATURE);
    sequenceCount = getIntent().getLongExtra(
        ContribInfoDialogFragment.KEY_SEQUENCE_COUNT, -1);

    if (savedInstanceState == null) {
      if (f == null) {
        ContribInfoDialogFragment.newInstance(sequenceCount)
            .show(getSupportFragmentManager(), "CONTRIB_INFO");
      } else {
        ContribDialogFragment.newInstance(
            f, getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(), "CONTRIB");
      }
    }
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.REMIND_LATER_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(
            sequenceCount + MyExpenses.TRESHOLD_REMIND_CONTRIB);
        break;
      case R.id.REMIND_NO_CONTRIB_COMMAND:
        PrefKey.NEXT_REMINDER_CONTRIB.putLong(-1);
    }
    finish();
    return true;
  }

  public void contribBuyDo(boolean extended) {
    DonateDialogFragment.newInstance(extended).show(getSupportFragmentManager(), "CONTRIB");
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish();
  }

  @Override
  public void finish() {
    final ContribFeature feature = (ContribFeature) getIntent().getSerializableExtra(KEY_FEATURE);
    if (feature != null) {
      int usagesLeft = feature.usagesLeft();
      Intent i = new Intent();
      i.putExtra(KEY_FEATURE, feature);
      i.putExtra(KEY_TAG, getIntent().getSerializableExtra(KEY_TAG));
      if (usagesLeft > 0) {
        setResult(RESULT_OK, i);
      } else {
        setResult(RESULT_CANCELED, i);
      }
    }
    super.finish();
  }

  @Override
  protected void onActivityResult(int arg0, int arg1, Intent arg2) {
   finish();
  }
}
