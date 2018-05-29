package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.view.ViewGroup;

import com.squareup.phrase.Phrase;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;
import static org.totschnig.myexpenses.util.Utils.PLACEHOLDER_APP_NAME;

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
          Phrase.from(context, R.string.gdpr_consent_message)
              .put(PLACEHOLDER_APP_NAME, context.getString(R.string.app_name))
              .put("ad_provider", "PubNative")
              .format(),
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
