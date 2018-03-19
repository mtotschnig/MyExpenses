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
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
  private String mPayload;
  private LicenceHandler licenceHandler;

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
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    String packageFromExtra = getIntent().getStringExtra(KEY_PACKAGE);
    licenceHandler = MyApplication.getInstance().getLicenceHandler();
    mPayload = licenceHandler.getPayLoad();
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
    Integer[] paymentOptions = aPackage.getPaymentOptions();
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
            new IabHelper.OnIabPurchaseFinishedListener() {
              public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Timber.d("Purchase finished: %s, purchase: %s", result, purchase);
                if (result.isFailure()) {
                  Timber.w("Purchase failed: %s, purchase: %s", result, purchase);
                  showMessage(getString(R.string.premium_failed_or_canceled));
                } else if (!verifyDeveloperPayload(purchase)) {
                  showMessage("Error purchasing. Authenticity verification failed.");
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
              }

              private boolean verifyDeveloperPayload(Purchase purchase) {
                if (mPayload == null) {
                  return true;
                }
                String payload = purchase.getDeveloperPayload();
                return payload != null && payload.equals(mPayload);
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
              mPayload
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
    Timber.e("**** InAppPurchase Error: %s", message);
    ContribDialogFragment fragment = ((ContribDialogFragment) getSupportFragmentManager().findFragmentByTag("CONTRIB"));
    if (fragment != null) {
      fragment.showSnackbar(message, Snackbar.LENGTH_LONG, false);
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
        String host = BuildConfig.DEBUG ? "www.sandbox.paypal.com" : "www.paypal.com";
        String paypalButtonId = BuildConfig.DEBUG ? "TURRUESSCUG8N" : "LBUDF8DSWJAZ8";
        String uri = String.format(Locale.US,
            "https://%s/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=%s&on0=%s&os0=%s&lc=%s",
            host, paypalButtonId, "Licence", aPackage.name(), getPaypalLocale());
        String licenceEmail = PrefKey.LICENCE_EMAIL.getString(null);
        if (licenceEmail != null) {
          uri += "&custom=" + Uri.encode(licenceEmail);
        }

        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(uri));
        startActivityForResult(intent, 0);
        break;
      }
      case R.string.donate_button_invoice: {
        intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{MyApplication.INVOICES_EMAIL});
        String packageLabel = aPackage.getButtonLabel(this);
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

  private String getPaypalLocale() {
    Locale locale = Locale.getDefault();
    switch (locale.getLanguage()) {
      case "en":
        return "en_US";
      case "fr":
        return "fr_FR";
      case "es":
        return "es_ES";
      case "zh":
        return "zh_CN";
      case "ar":
        return "ar_EG";
      case "de":
        return "de_DE";
      case "nl":
        return "nl_NL";
      case "pt":
        return "pt_PT";
      case "da":
        return "da_DK";
      case "ru":
        return "ru_RU";
      case "id":
        return "id_ID";
      case "iw":
      case "he":
        return "he_IL";
      case "it":
        return "it_IT";
      case "ja":
        return "ja_JP";
      case "no":
        return "no_NO";
      case "pl":
        return "pl_PL";
      case "ko":
        return "ko_KO";
      case "sv":
        return "sv_SE";
      case "th":
        return "th_TH";
      default:
        return "en_US";
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
      int usagesLeft = feature.usagesLeft();
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
