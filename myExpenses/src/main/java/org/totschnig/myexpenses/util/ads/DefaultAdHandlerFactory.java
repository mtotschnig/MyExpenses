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

import java.util.Arrays;

import static org.totschnig.myexpenses.preference.PrefKey.DEBUG_ADS;
import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;
import static org.totschnig.myexpenses.util.Utils.PLACEHOLDER_APP_NAME;

public class DefaultAdHandlerFactory implements AdHandlerFactory {
  private static final int INITIAL_GRACE_DAYS = 5;
  private static final String[] EU_COUNTRIES = {
       "at",
       "be",
       "bg",
       "cy",
       "cz",
       "de",
       "dk",
       "ee",
       "el",
       "es",
       "fi",
       "fr",
       "hr",
       "hu",
       "ie",
       "it",
       "lt",
       "lu",
       "lv",
       "mt",
       "nl",
       "pl",
       "pt",
       "ro",
       "se",
       "si",
       "sk",
       "uk"
  };
  protected final Context context;
  protected final PrefHandler prefHandler;
  protected final String userCountry;

  public DefaultAdHandlerFactory(Context context, PrefHandler prefHandler, String userCountry) {
    this.context = context;
    this.prefHandler = prefHandler;
    this.userCountry = userCountry;
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
    return Arrays.binarySearch(EU_COUNTRIES, userCountry) >= 0;
  }

  @Override
  public AdHandler create(ViewGroup adContainer) {
    return isAdDisabled() ? new NoOpAdHandler(this, adContainer) :
        new CustomAdHandler(this, adContainer, userCountry);
  }

  @Override
  public void gdprConsent(ProtectedFragmentActivity context, boolean forceShow) {
    if (forceShow || (!isAdDisabled() && isRequestLocationInEeaOrUnknown() && !prefHandler.isSet(PERSONALIZED_AD_CONSENT))) {
      int positiveString;
      MessageDialogFragment.Button neutral = null;
      String adProviders = "Google";
      positiveString = R.string.pref_ad_consent_title;
      neutral = new MessageDialogFragment.Button(R.string.ad_consent_non_personalized, R.id.GDPR_CONSENT_COMMAND, false);
      MessageDialogFragment.Button positive = new MessageDialogFragment.Button(positiveString, R.id.GDPR_CONSENT_COMMAND, true);

      MessageDialogFragment.newInstance(
          null,
          Phrase.from(context, R.string.gdpr_consent_message)
              .put(PLACEHOLDER_APP_NAME, context.getString(R.string.app_name))
              .put("ad_provider", adProviders)
              .format(),
          positive,
          neutral,
          new MessageDialogFragment.Button(R.string.gdpr_consent_button_no, R.id.GDPR_NO_CONSENT_COMMAND, null))
          .show(context.getSupportFragmentManager(), "MESSAGE");
    }
  }

  @Override
  public void clearConsent() {
    prefHandler.remove(PERSONALIZED_AD_CONSENT);
  }

  @Override
  public void setConsent(Context context, boolean personalized) {
    prefHandler.putBoolean(PERSONALIZED_AD_CONSENT, personalized);
  }
}
