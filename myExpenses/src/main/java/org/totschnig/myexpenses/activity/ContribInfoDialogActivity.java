package org.totschnig.myexpenses.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.ShortcutHelper;

import java.io.Serializable;

/**
 * Manages the dialog shown to user when they request usage of a premium functionality or click on
 * the dedicated entry on the preferences screen. If called from an activity extending
 * {@link ProtectedFragmentActivity}, {@link ContribIFace#contribFeatureCalled(ContribFeature, Serializable)}
 * or {@link ContribIFace#contribFeatureNotCalled(ContribFeature)} will be triggered on it, depending on
 * if user canceled or has usages left. If called from shortcut, this activity will launch the intent
 * for the premium feature directly
 */
public class ContribInfoDialogActivity extends ProtectedFragmentActivity
    implements MessageDialogListener {
  public final static String KEY_FEATURE = "feature";
  public static final String KEY_TAG = "tag";

  public static Intent getIntentFor(Context context, ContribFeature feature) {
    Intent intent = new Intent(context, ContribInfoDialogActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.putExtra(KEY_FEATURE, feature.name());
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      ContribDialogFragment.newInstance(getIntent().getStringExtra(KEY_FEATURE),
          getIntent().getSerializableExtra(KEY_TAG))
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
    String featureStringFromExtra = getIntent().getStringExtra(KEY_FEATURE);
    if (featureStringFromExtra != null) {
      ContribFeature feature = ContribFeature.valueOf(featureStringFromExtra);
      int usagesLeft = feature.usagesLeft();
      if (callerIsContribIface()) {
        Intent i = new Intent();
        i.putExtra(KEY_FEATURE, featureStringFromExtra);
        i.putExtra(KEY_TAG, getIntent().getSerializableExtra(KEY_TAG));
        if (!canceled && usagesLeft > 0) {
          setResult(RESULT_OK, i);
        } else {
          setResult(RESULT_CANCELED, i);
        }
      } else {
        callFeature(feature);
      }
    }
    super.finish();
  }

  private void callFeature(ContribFeature feature) {
    switch (feature) {
      case SPLIT_TRANSACTION:
        startActivity(ShortcutHelper.createIntentForNewSplit(this));
      break;
      default:
        //should not happen
        AcraHelper.report(new IllegalStateException(
            String.format("Unhandlable request for feature %s", feature)));
    }
  }

  private boolean callerIsContribIface() {
    boolean result = false;
    ComponentName callingActivity = getCallingActivity();
    if (callingActivity != null) {
      try {
        Class<?> caller = Class.forName(callingActivity.getClassName());
        result = ContribIFace.class.isAssignableFrom(caller);
      } catch (ClassNotFoundException ignored) {}
    }
    return result;
  }

  @Override
  protected void onActivityResult(int arg0, int arg1, Intent arg2) {
    finish(false);
  }
}
