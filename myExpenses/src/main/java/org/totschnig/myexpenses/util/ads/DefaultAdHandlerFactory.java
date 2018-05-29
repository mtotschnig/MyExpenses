package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.view.ViewGroup;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;

public class DefaultAdHandlerFactory implements AdHandlerFactory {
  protected final Context context;
  protected final PrefHandler prefHandler;

  public DefaultAdHandlerFactory(Context context, PrefHandler prefHandler) {
    this.context = context;
    this.prefHandler = prefHandler;
  }

  @Override
  public boolean isRequestLocationInEeaOrUnknown() {
    return true;
  }

  @Override
  public AdHandler create(ViewGroup adContainer) {
    return (!AdHandler.isAdDisabled(context, prefHandler) &&
        Utils.isComAndroidVendingInstalled(context)) ?
        new PubNativeAdHandler(adContainer) :
        new NoOpAdHandler(adContainer);
  }

  @Override
  public void gdprConsent(ProtectedFragmentActivity context, boolean forceShow) {
    if (forceShow || !prefHandler.isSet(PERSONALIZED_AD_CONSENT)) {
      MessageDialogFragment.newInstance(
          0,
          "We partner with %s in order to sustain the support, maintenance and development of {app_name} by showing you ads.\n" +
              "We would like to ask for your consent. If you prefer to not see any ads, please pay for the ad-free version.",
          new MessageDialogFragment.Button(R.string.gdpr_consent_button, R.id.GDPR_CONSENT_COMMAND, null),
          null, new MessageDialogFragment.Button(R.string.contrib_feature_ad_free_label, R.id.GDPR_NO_CONSENT_COMMAND, null))
          .show(context.getSupportFragmentManager(), "MESSAGE");
    }
  }

  @Override
  public void clearConsent() {
    prefHandler.remove(PERSONALIZED_AD_CONSENT);
  }

  @Override
  public void setConsent(boolean personalized) {
    prefHandler.putBoolean(PERSONALIZED_AD_CONSENT, personalized);
  }
}
