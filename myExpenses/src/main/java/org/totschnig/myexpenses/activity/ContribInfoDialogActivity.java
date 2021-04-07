package org.totschnig.myexpenses.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.BillingListener;
import org.totschnig.myexpenses.util.licence.BillingManager;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.io.Serializable;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ConstantsKt.INVOICE_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.PAYPAL_REQUEST;

/**
 * Manages the dialog shown to user when they request usage of a premium functionality or click on
 * the dedicated entry on the preferences screen. If called from an activity extending
 * {@link ProtectedFragmentActivity}, {@link ContribIFace#contribFeatureCalled(ContribFeature, Serializable)}
 * or {@link ContribIFace#contribFeatureNotCalled(ContribFeature)} will be triggered on it, depending on
 * if user canceled or has usages left. If called from shortcut, this activity will launch the intent
 * for the premium feature directly
 */
public class ContribInfoDialogActivity extends ProtectedFragmentActivity
    implements BillingListener {
  public final static String KEY_FEATURE = "feature";
  private final static String KEY_PACKAGE = "package";
  public static final String KEY_TAG = "tag";
  private static final String KEY_SHOULD_REPLACE_EXISTING = "shouldReplaceExisting";
  private boolean doFinishAfterMessageDismiss = true;

  @Nullable
  private BillingManager billingManager;

  public static Intent getIntentFor(Context context, @Nullable ContribFeature feature) {
    Intent intent = new Intent(context, ContribInfoDialogActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    if (feature != null) {
      intent.putExtra(KEY_FEATURE, feature.name());
    }
    return intent;
  }

  public static Intent getIntentFor(Context context, @NonNull Package aPackage, boolean shouldReplaceExisting) {
    Intent intent = new Intent(context, ContribInfoDialogActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.putExtra(KEY_PACKAGE, aPackage);
    intent.putExtra(KEY_SHOULD_REPLACE_EXISTING, shouldReplaceExisting);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Package packageFromExtra = packageFromExtra();

    if (savedInstanceState == null) {
      if (packageFromExtra == null) {
        ContribDialogFragment.newInstance(getIntent().getStringExtra(KEY_FEATURE),
            getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(), "CONTRIB");
        getSupportFragmentManager().executePendingTransactions();
      } else {
        if (DistributionHelper.isGithub()) {
          contribBuyDo(packageFromExtra);
        }
      }
    }
    billingManager = licenceHandler.initBillingManager(this, false);
  }

  private Package packageFromExtra() {
    return getIntent().getParcelableExtra(KEY_PACKAGE);
  }

  private void contribBuyGithub(Package aPackage) {
    int[] paymentOptions = licenceHandler.getPaymentOptions(aPackage);
    if (paymentOptions.length > 1) {
      DonateDialogFragment.newInstance(aPackage).show(getSupportFragmentManager(), "CONTRIB");
    } else {
      startPayment(paymentOptions[0], aPackage);
    }
  }

  public void contribBuyDo(@NonNull Package aPackage) {
    Bundle bundle = new Bundle(1);
    bundle.putString(Tracker.EVENT_PARAM_PACKAGE, aPackage.getClass().getSimpleName());
    logEvent(Tracker.EVENT_CONTRIB_DIALOG_BUY, bundle);
    switch (DistributionHelper.getDistribution()) {
      case PLAY:
      case AMAZON:
        try {
          if (billingManager != null) {
            licenceHandler.launchPurchase(aPackage, getIntent().getBooleanExtra(KEY_SHOULD_REPLACE_EXISTING, false), billingManager);
          }
        } catch (IllegalStateException e) {
          CrashHandler.report(e);
          showMessage(e.getMessage() != null ? e.getMessage() : "ERROR");
        }
        break;
      default:
        contribBuyGithub(aPackage);
        break;
    }
  }

  void complain(String message) {
    CrashHandler.report(String.format("**** InAppPurchase Error: %s", message));
    showMessage(message);
  }

  @Override
  protected int getSnackbarContainerId() {
    return android.R.id.content;
  }

  public void startPayment(int paymentOption, Package aPackage) {
    Intent intent;
    if (paymentOption == R.string.donate_button_paypal) {
      intent = new Intent(Intent.ACTION_VIEW);
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setData(Uri.parse(licenceHandler.getPaypalUri(aPackage)));
      try {
        startActivityForResult(intent, PAYPAL_REQUEST);
      } catch (ActivityNotFoundException e) {
        complain("No activity found for opening Paypal");
      }
    } else if (paymentOption == R.string.donate_button_invoice) {
      intent = new Intent(Intent.ACTION_SEND);
      intent.setType("message/rfc822");
      intent.putExtra(Intent.EXTRA_EMAIL, new String[]{MyApplication.INVOICES_EMAIL});
      String packageLabel = licenceHandler.getButtonLabel(aPackage);
      intent.putExtra(Intent.EXTRA_SUBJECT,
          "[" + getString(R.string.app_name) + "] " + getString(R.string.donate_button_invoice));
      String userCountry = Utils.getCountryFromTelephonyManager(this);
      String messageBody = String.format(
          "Please send an invoice for %s to:\nName: (optional)\nCountry: %s (required)",
          packageLabel, userCountry != null ? userCountry : "");
      intent.putExtra(Intent.EXTRA_TEXT, messageBody);
      startActivity(intent, R.string.no_app_handling_email_available, INVOICE_REQUEST);
    }
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    if (doFinishAfterMessageDismiss) {
      finish(true);
    }
  }

  public void finish(boolean canceled) {
    String featureStringFromExtra = getIntent().getStringExtra(KEY_FEATURE);
    if (featureStringFromExtra != null) {
      ContribFeature feature = ContribFeature.valueOf(featureStringFromExtra);
      int usagesLeft = feature.usagesLeft(prefHandler);
      boolean shouldCallFeature = licenceHandler.hasAccessTo(feature) || (!canceled && usagesLeft > 0);
      if (callerIsContribIface()) {
        Intent i = new Intent();
        i.putExtra(KEY_FEATURE, featureStringFromExtra);
        i.putExtra(KEY_TAG, getIntent().getSerializableExtra(KEY_TAG));
        if (shouldCallFeature) {
          setResult(RESULT_OK, i);
        } else {
          setResult(RESULT_CANCELED, i);
        }
      } else if (shouldCallFeature) {
        callFeature(feature);
      }
    }
    super.finish();
  }

  private void callFeature(ContribFeature feature) {
    if (feature == ContribFeature.SPLIT_TRANSACTION) {
      startActivity(ShortcutHelper.createIntentForNewSplit(this));
    }
    // else User bought licence in the meantime
  }

  private boolean callerIsContribIface() {
    boolean result = false;
    ComponentName callingActivity = getCallingActivity();
    if (callingActivity != null) {
      try {
        Class<?> caller = Class.forName(callingActivity.getClassName());
        result = ContribIFace.class.isAssignableFrom(caller);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return result;
  }

  @Override
  public void onLicenceStatusSet(String newStatus) {
    if (newStatus != null) {
      Timber.d("Purchase is premium upgrade. Congratulating user.");
      showMessage(
          String.format("%s (%s) %s", getString(R.string.licence_validation_premium),
              newStatus, getString(R.string.thank_you)));
    } else {
      complain("Validation of purchase failed");
    }
  }

  public void onPurchaseCancelled() {
    showMessage(getString(R.string.premium_failed_or_canceled));
  }

  public void onPurchaseFailed(int code) {
    showMessage(String.format(Locale.ROOT, "%s (%d)", getString(R.string.premium_failed_or_canceled), code));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (billingManager != null) {
      billingManager.destroy();
    }
    billingManager = null;
  }

  @Override
  public void onBillingSetupFinished() {
    Package packageFromExtra = packageFromExtra();
    if (packageFromExtra != null) {
      contribBuyDo(packageFromExtra);
    }
  }

  @Override
  public void onBillingSetupFailed(@NonNull String reason) {
    if (DistributionHelper.isPlay()) {
      doFinishAfterMessageDismiss = false;
      complain(String.format("Billing setup failed (%s)", reason));
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == PAYPAL_REQUEST || requestCode == INVOICE_REQUEST) {
      finish(false);
    }
  }
}
