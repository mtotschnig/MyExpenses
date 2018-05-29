package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.view.ViewGroup;

import com.squareup.phrase.Phrase;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.preference.PrefKey.DEBUG_ADS;
import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;
import static org.totschnig.myexpenses.util.Utils.PLACEHOLDER_APP_NAME;

public class DefaultAdHandlerFactory implements AdHandlerFactory {
  private static final int INITIAL_GRACE_DAYS = 5;
  protected final Context context;
  protected final PrefHandler prefHandler;

  DefaultAdHandlerFactory(Context context, PrefHandler prefHandler) {
    this.context = context;
    this.prefHandler = prefHandler;
  }

  @Override
  public boolean isAdDisabled() {
    return !prefHandler.getBoolean(DEBUG_ADS, false) &&
        (ContribFeature.AD_FREE.hasAccess() ||
            isInInitialGracePeriod() || BuildConfig.DEBUG);
  }

  private boolean isInInitialGracePeriod() {
    return Utils.getDaysSinceInstall(context) < INITIAL_GRACE_DAYS;
  }

  @Override
  public boolean isRequestLocationInEeaOrUnknown() {
    return true;
  }

  @Override
  public AdHandler create(ViewGroup adContainer) {
    return (!isAdDisabled() &&
        Utils.isComAndroidVendingInstalled(context)) ?
        new PubNativeAdHandler(this, adContainer) :
        new NoOpAdHandler();
  }

  @Override
  public void gdprConsent(ProtectedFragmentActivity context, boolean forceShow) {
    if (forceShow || !prefHandler.isSet(PERSONALIZED_AD_CONSENT)) {
      MessageDialogFragment.newInstance(
          0,
          Phrase.from(context, R.string.gdpr_consent_message)
              .put(PLACEHOLDER_APP_NAME, context.getString(R.string.app_name))
              .put("ad_provider", "PubNative")
              .format(),
          new MessageDialogFragment.Button(R.string.gdpr_consent_button_yes, R.id.GDPR_CONSENT_COMMAND, null),
          null, new MessageDialogFragment.Button(R.string.gdpr_consent_button_no, R.id.GDPR_NO_CONSENT_COMMAND, null))
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
