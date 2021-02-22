package org.totschnig.myexpenses.util.licence;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.apache.commons.lang3.time.DateUtils;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.Preconditions;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

import static androidx.annotation.RestrictTo.Scope.TESTS;

public class LicenceHandler {
  private boolean hasOurLicence = false;
  protected static final String LICENSE_STATUS_KEY = "licence_status";
  private static final String LICENSE_VALID_SINCE_KEY = "licence_valid_since";
  private static final String LICENSE_VALID_UNTIL_KEY = "licence_valid_until";
  public static final String TAG = "LicenceHandler";
  protected final MyApplication context;
  private final CrashHandler crashHandler;
  private boolean isSandbox = BuildConfig.DEBUG;

  @Nullable private LicenceStatus licenceStatus;
  PreferenceObfuscator licenseStatusPrefs;
  CurrencyUnit currencyUnit;

  public LicenceHandler(MyApplication context, PreferenceObfuscator preferenceObfuscator, CrashHandler crashHandler) {
    this.context = context;
    this.licenseStatusPrefs = preferenceObfuscator;
    this.currencyUnit = new CurrencyUnit("EUR", "€", 2);
    this.crashHandler = crashHandler;
  }

  public boolean hasValidKey() {
    return isContribEnabled() && !hasLegacyLicence();
  }

  public boolean isContribEnabled() {
    return isEnabledFor(LicenceStatus.CONTRIB);
  }

  @VisibleForTesting
  public boolean isExtendedEnabled() {
    return isEnabledFor(LicenceStatus.EXTENDED);
  }

  public boolean isProfessionalEnabled() {
    return isEnabledFor(LicenceStatus.PROFESSIONAL);
  }

  public boolean isEnabledFor(@NonNull LicenceStatus licenceStatus) {
    if (this.licenceStatus == null) {
      return false;
    }
    return this.licenceStatus.ordinal() >= licenceStatus.ordinal();
  }

  public boolean isUpgradeable() {
    return licenceStatus == null || licenceStatus.isUpgradeable();
  }

  public void init() {
    String licenseStatusPrefsString = licenseStatusPrefs.getString(LICENSE_STATUS_KEY, null);
    try {
      final LicenceStatus licenceStatus = licenseStatusPrefsString != null ? LicenceStatus.valueOf(licenseStatusPrefsString) : null;
      if (licenceStatus != null) {
        hasOurLicence = true;
      }
      setLicenceStatusInternal(licenceStatus);
    } catch (IllegalArgumentException e) {
      setLicenceStatusInternal(null);
    }
  }

  public final void update() {
    Template.updateNewPlanEnabled();
    Account.updateNewAccountEnabled();
    GenericAccountService.updateAccountsIsSyncable(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      ShortcutHelper.configureSplitShortcut(context, isContribEnabled());
    }
  }

  public void updateLicenceStatus(Licence licence) {
    if (licence == null || licence.getType() == null) {
      setLicenceStatusInternal(null);
      licenseStatusPrefs.remove(LICENSE_STATUS_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
    } else {
      hasOurLicence = true;
      setLicenceStatusInternal(licence.getType());
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      if (licence.getValidSince() != null) {
        ZonedDateTime validSince = licence.getValidSince().atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT-14"));
        licenseStatusPrefs.putString(LICENSE_VALID_SINCE_KEY, String.valueOf(validSince.toEpochSecond() * 1000));
      }
      if (licence.getValidUntil() != null) {
        ZonedDateTime validUntil = licence.getValidUntil().atTime(LocalTime.MAX).atZone(ZoneId.of("Etc/GMT+12"));
        licenseStatusPrefs.putString(LICENSE_VALID_UNTIL_KEY, String.valueOf(validUntil.toEpochSecond() * 1000));
      } else {
        licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
      }
    }
    licenseStatusPrefs.commit();
    update();
  }

  public void reset() {
    init();
    update();
  }

  public static Timber.Tree log() {
    return Timber.tag(TAG);
  }

  @RestrictTo(TESTS)
  public void setLockState(boolean locked) {
    setLicenceStatusInternal(locked ? null : LicenceStatus.PROFESSIONAL);
    update();
  }

  @Nullable
  public String getFormattedPrice(Package aPackage) {
    return getFormattedPriceWithExtra(aPackage, false);
  }

  @Nullable
  String getFormattedPriceWithExtra(Package aPackage, boolean withExtra) {
    return aPackage.getFormattedPrice(context, currencyUnit, withExtra);
  }

  public String getFormattedPriceWithSaving(Package aPackage) {
    final boolean withExtra = licenceStatus == LicenceStatus.EXTENDED;
    String formattedPrice = getFormattedPriceWithExtra(aPackage, withExtra);
    final Package base = Package.Professional_6;
    if (aPackage == base) return formattedPrice;
    return String.format(Locale.ROOT, "%s (- %d %%)", formattedPrice,
        100 - (aPackage.getDefaultPrice() * 100 * base.getDuration(withExtra) /
            (aPackage.getDuration(withExtra) * base.getDefaultPrice())));
  }

  public String getExtendOrSwitchMessage(Package aPackage) {
    Preconditions.checkArgument(aPackage.isProfessional());
    Date extendedDate = DateUtils.addMonths(
        new Date(Math.max(getValidUntilMillis(), System.currentTimeMillis())),
        aPackage.getDuration(false));
    return context.getString(R.string.extend_until,
        Utils.getDateFormatSafe(context).format(extendedDate),
        aPackage.getFormattedPriceRaw(currencyUnit, context));
  }

  @NonNull
  public String getProLicenceStatus(Context context) {
    return getProValidUntil(context);
  }

  String getProValidUntil(Context context) {
    return context.getString(R.string.valid_until, Utils.getDateFormatSafe(this.context).format(getValidUntilDate()));
  }

  @NonNull
  private Date getValidUntilDate() {
    return new Date(getValidUntilMillis());
  }

  public long getValidUntilMillis() {
    return Long.parseLong(licenseStatusPrefs.getString(LICENSE_VALID_UNTIL_KEY, "0"));
  }


  public long getValidSinceMillis() {
    return Long.parseLong(licenseStatusPrefs.getString(LICENSE_VALID_SINCE_KEY, "0"));
  }

  public boolean hasLegacyLicence() {
    return false;
  }

  public boolean needsMigration() {
    return false;
  }

  public Package[] getProPackages() {
    return new Package[]{Package.Professional_6, Package.Professional_12, Package.Professional_24};
  }

  @Nullable
  public String getExtendedUpgradeGoodieMessage(Package selectedPackage) {
    return context.getString(R.string.extended_upgrade_goodie_github, 3);
  }

  public String getProfessionalPriceShortInfo() {
    return joinPriceInfos(getProPackages());
  }

  protected String joinPriceInfos(Package... packages) {
    return Stream.of(packages)
        .map(this::getFormattedPrice)
        .collect(Collectors.joining(String.format(" %s ", context.getString(R.string.joining_or))));
  }

  @Nullable
  public Package[] getProPackagesForExtendOrSwitch() {
    return getProPackages();
  }


  @NonNull
  public String getProLicenceAction(Context context) {
    return context.getString(R.string.extend_validity);
  }

  @Nullable
  public String getPurchaseExtraInfo() {
    return null;
  }

  public String buildRoadmapVoteKey() {
    return UUID.randomUUID().toString();
  }

  /**
   * @return true if licenceStatus has been upEd
   */
  public boolean registerUnlockLegacy() {
    return false;
  }

  /**
   * @return true if licenceStatus has been upEd
   */
  public boolean registerBlackberryProfessional() {
    return false;
  }

  public int[] getPaymentOptions(Package aPackage) {
    return (aPackage.getDefaultPrice() >= 500) ?
        new int[]{R.string.donate_button_paypal, R.string.donate_button_invoice} :
        new int[]{R.string.donate_button_paypal};
  }

  public boolean doesUseIAP() {
    return false;
  }

  public boolean needsKeyEntry() {
    return true;
  }

  public String getPaypalUri(Package aPackage) {
    String host = isSandbox ? "www.sandbox.paypal.com" : "www.paypal.com";
    String paypalButtonId = isSandbox ? "TURRUESSCUG8N" : "LBUDF8DSWJAZ8";
    String uri = String.format(Locale.US,
        "https://%s/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=%s&on0=%s&os0=%s&lc=%s&currency_code=EUR",
        host, paypalButtonId, "Licence", aPackage.name(), getPaypalLocale());
    String licenceEmail = PrefKey.LICENCE_EMAIL.getString(null);
    if (licenceEmail != null) {
      uri += "&custom=" + Uri.encode(licenceEmail);
    }
    return uri;
  }

  public String getBackendUri() {
    return isSandbox ? "https://myexpenses-licencedb-staging.herokuapp.com" : "https://licencedb.myexpenses.mobi/";
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

  public void handleExpiration() {
    long licenceDuration = getValidUntilMillis() - getValidSinceMillis();
    if (TimeUnit.MILLISECONDS.toDays(licenceDuration) > 240) { // roughly eight months
      setLicenceStatusInternal(LicenceStatus.EXTENDED);
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
      licenseStatusPrefs.commit();
    } else {
      updateLicenceStatus(null);
    }
  }

  public String getButtonLabel(Package aPackage) {
    int resId;
    switch (aPackage) {
      case Contrib:
        resId = LicenceStatus.CONTRIB.getResId();
        break;
      case Upgrade:
        resId = R.string.pref_contrib_purchase_title_upgrade;
        break;
      case Extended:
        resId = LicenceStatus.EXTENDED.getResId();
        break;
      default:
        resId = LicenceStatus.PROFESSIONAL.getResId();
    }
    return String.format("%s (%s)", context.getString(resId), getFormattedPriceWithExtra(aPackage, licenceStatus == LicenceStatus.EXTENDED));
  }

  void setLicenceStatus(@Nullable LicenceStatus licenceStatus) {
    if (!hasOurLicence || this.licenceStatus == null || !this.licenceStatus.greaterOrEqual(licenceStatus)) {
      setLicenceStatusInternal(licenceStatus);
    }
  }

  private void setLicenceStatusInternal(@Nullable LicenceStatus licenceStatus) {
    this.licenceStatus = licenceStatus;
    crashHandler.putCustomData("Licence", licenceStatus != null ? licenceStatus.name() : "null");
  }

  @Nullable public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }

  public BillingManager initBillingManager(@NonNull Activity activity, boolean query) {
    return null;
  }

  public void launchPurchase(@NonNull Package aPackage, boolean shouldReplaceExisting, @NonNull BillingManager billingManager) {
  }
}