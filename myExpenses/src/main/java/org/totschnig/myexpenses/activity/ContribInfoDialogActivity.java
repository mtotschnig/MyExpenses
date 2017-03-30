package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature;

public class ContribInfoDialogActivity extends ProtectedFragmentActivity
    implements MessageDialogListener {
  public final static String KEY_FEATURE = "feature";
  public static final String KEY_TAG = "tag";

  public static Intent getIntentFor(Context context, ContribFeature feature) {
    Intent intent = new Intent(context, ContribInfoDialogActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.putExtra(KEY_FEATURE, ContribFeature.SPLIT_TRANSACTION);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    ContribFeature f = (ContribFeature) getIntent().getSerializableExtra(KEY_FEATURE);

    if (savedInstanceState == null) {
      ContribDialogFragment.newInstance(
          f, getIntent().getSerializableExtra(KEY_TAG))
          .show(getSupportFragmentManager(), "CONTRIB");
    }
  }

  public void contribBuyDo(boolean extended) {
    DonateDialogFragment.newInstance(extended).show(getSupportFragmentManager(), "CONTRIB");
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish(true);
  }

  public void finish(boolean canceled) {
    final ContribFeature feature = (ContribFeature) getIntent().getSerializableExtra(KEY_FEATURE);
    if (feature != null) {
      int usagesLeft = feature.usagesLeft();
      Intent i = new Intent();
      i.putExtra(KEY_FEATURE, feature);
      i.putExtra(KEY_TAG, getIntent().getSerializableExtra(KEY_TAG));
      if (!canceled && usagesLeft > 0) {
        setResult(RESULT_OK, i);
      } else {
        setResult(RESULT_CANCELED, i);
      }
    }
    super.finish();
  }

  @Override
  protected void onActivityResult(int arg0, int arg1, Intent arg2) {
    finish(false);
  }
}
