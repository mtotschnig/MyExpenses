package org.totschnig.myexpenses.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static org.onepf.oms.OpenIabHelper.ITEM_TYPE_INAPP;
import static org.onepf.oms.OpenIabHelper.ITEM_TYPE_SUBS;


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
  private final static String KEY_PACKAGE = "package";
  public static final String KEY_TAG = "tag";
  private static final String KEY_SHOULD_REPLACE_EXISTING = "shouldReplaceExisting";
  private OpenIabHelper mHelper;
  private boolean mSetupDone;

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
    intent.putExtra(KEY_PACKAGE, aPackage.name());
    intent.putExtra(KEY_SHOULD_REPLACE_EXISTING, shouldReplaceExisting);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    String packageFromExtra = getIntent().getStringExtra(KEY_PACKAGE);
    mHelper = licenceHandler.getIabHelper(this);

    if (mHelper != null) {
      try {
        mHelper.startSetup(result -> {
          Timber.d("Setup finished.");

          if (!result.isSuccess()) {
            mSetupDone = false;
            // Oh noes, there was a problem.
            complain("Problem setting up in-app billing: " + result);
            return;
          }
          mSetupDone = true;
          Timber.d("Setup successful.");
          if (packageFromExtra != null) {
            contribBuyDo(Package.valueOf(packageFromExtra));
          }
        });
      } catch (SecurityException e) {
        CrashHandler.report(e);
        mHelper.dispose();
        mHelper = null;
        complain("Problem setting up in-app billing: " + e.getMessage());
      }
    }

    if (savedInstanceState == null) {
      if (packageFromExtra == null) {
        ContribDialogFragment.newInstance(getIntent().getStringExtra(KEY_FEATURE),
            getIntent().getSerializableExtra(KEY_TAG))
            .show(getSupportFragmentManager(), "CONTRIB");
        getSupportFragmentManager().executePendingTransactions();
      } else {
        if (DistribHelper.isGithub()) {
          contribBuyDo(Package.valueOf(packageFromExtra));
        }
      }
    }
  }

  private void contribBuyGithub(Package aPackage) {
    int[] paymentOptions = licenceHandler.getPaymentOptions(aPackage);
    if (paymentOptions.length > 1) {
      DonateDialogFragment.newInstance(aPackage).show(getSupportFragmentManager(), "CONTRIB");
    } else {
      startPayment(paymentOptions[0], aPackage);
    }
  }

  public void contribBuyDo(Package aPackage) {
    Bundle bundle = new Bundle(1);
    bundle.putString(Tracker.EVENT_PARAM_PACKAGE, aPackage.name());
    logEvent(Tracker.EVENT_CONTRIB_DIALOG_BUY, bundle);
    switch (DistribHelper.getDistribution()) {
      case PLAY:
      case AMAZON:
        if (mHelper == null) {
          finish();
          return;
        }
        if (!mSetupDone) {
          complain("Billing setup is not completed yet");
          finish();
          return;
        }
        final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            (result, purchase) -> {
              Timber.d("Purchase finished: %s, purchase: %s", result, purchase);
              if (result.isFailure()) {
                Timber.w("Purchase failed: %s, purchase: %s", result, purchase);
                showMessage(getString(R.string.premium_failed_or_canceled));
              } else {
                Timber.d("Purchase successful.");

                LicenceStatus licenceStatus = licenceHandler.handlePurchase(purchase.getSku(), purchase.getOrderId());

                if (licenceStatus != null) {
                  // bought the premium upgrade!
                  Timber.d("Purchase is premium upgrade. Congratulating user.");
                  showMessage(
                      String.format("%s (%s) %s", getString(R.string.licence_validation_premium),
                          getString(licenceStatus.getResId()), getString(R.string.thank_you)));
                } else {
                  finish();
                }
              }
            };
        String sku = licenceHandler.getSkuForPackage(aPackage);

        List<String> oldSkus;
        if (getIntent().getBooleanExtra(KEY_SHOULD_REPLACE_EXISTING, false)) {
          String currentSubscription = licenceHandler.getCurrentSubscription();
          if (currentSubscription == null) {
            complain("Could not determine current subscription");
            finish();
            return;
          }
          oldSkus = Collections.singletonList(currentSubscription);
        } else {
          oldSkus = null;
        }

        try {
          mHelper.launchPurchaseFlow(
              ContribInfoDialogActivity.this,
              sku, aPackage.isProfessional() ? ITEM_TYPE_SUBS : ITEM_TYPE_INAPP,
              oldSkus,
              ProtectedFragmentActivity.PURCHASE_PREMIUM_REQUEST,
              mPurchaseFinishedListener,
              null
          );
        } catch (IabHelper.IabAsyncInProgressException e) {
          complain("Another async operation in progress.");
        }
        break;
      case BLACKBERRY:
      case GITHUB:
        contribBuyGithub(aPackage);
        break;
    }
  }

  void complain(String message) {
    CrashHandler.report(String.format("**** InAppPurchase Error: %s", message));
    ContribDialogFragment fragment = ((ContribDialogFragment) getSupportFragmentManager().findFragmentByTag("CONTRIB"));
    if (fragment != null) {
      fragment.showSnackbar(message, Snackbar.LENGTH_LONG, null);
    } else {
      showSnackbar(message, Snackbar.LENGTH_LONG);
    }
  }

  @Override
  protected int getSnackbarContainerId() {
    return android.R.id.content;
  }

  public void startPayment(int paymentOption, Package aPackage) {
    Intent intent;
    switch (paymentOption) {
      case R.string.donate_button_paypal: {
        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(licenceHandler.getPaypalUri(aPackage)));
        startActivityForResult(intent, 0);
        break;
      }
      case R.string.donate_button_invoice: {
        intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{MyApplication.INVOICES_EMAIL});
        String packageLabel = licenceHandler.getButtonLabel(aPackage);
        intent.putExtra(Intent.EXTRA_SUBJECT,
            "[" + getString(R.string.app_name) + "] " + getString(R.string.donate_button_invoice));
        String userCountry = Utils.getCountryFromTelephonyManager();
        String messageBody = String.format(
            "Please send an invoice for %s to:\nName: (optional)\nCountry: %s (required)",
            packageLabel, userCountry != null ? userCountry : "");
        intent.putExtra(Intent.EXTRA_TEXT, messageBody);
        if (!Utils.isIntentAvailable(this, intent)) {
          complain(getString(R.string.no_app_handling_email_available));
        } else {
          startActivityForResult(intent, 0);
        }
      }
    }
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    finish(true);
  }

  public void finish(boolean canceled) {
    String featureStringFromExtra = getIntent().getStringExtra(KEY_FEATURE);
    if (featureStringFromExtra != null) {
      ContribFeature feature = ContribFeature.valueOf(featureStringFromExtra);
      int usagesLeft = feature.usagesLeft(getPrefHandler());
      boolean shouldCallFeature = feature.hasAccess() || (!canceled && usagesLeft > 0);
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
    switch (feature) {
      case SPLIT_TRANSACTION:
        startActivity(ShortcutHelper.createIntentForNewSplit(this));
        break;
      default:
        //should not happen
        CrashHandler.report(new IllegalStateException(
            String.format("Unhandlable request for feature %s (caller = %s)", feature,
                getCallingActivity() != null ? getCallingActivity().getClassName() : "null")));
    }
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Timber.d("onActivityResult() requestCode: %d resultCode: %d data: %s", requestCode, resultCode, data);

    // Pass on the activity result to the helper for handling
    if (mHelper == null || !mHelper.handleActivityResult(requestCode, resultCode, data)) {
      // not handled, so handle it ourselves (here's where you'd
      // perform any handling of activity results not related to in-app
      // billing...
      finish(false);
    } else {
      Timber.d("onActivityResult handled by IABUtil.");
    }
  }

  // We're being destroyed. It's important to dispose of the helper here!
  @Override
  public void onDestroy() {
    super.onDestroy();

    // very important:
    Timber.d("Destroying helper.");
    if (mHelper != null) mHelper.dispose();
    mHelper = null;
  }
}
