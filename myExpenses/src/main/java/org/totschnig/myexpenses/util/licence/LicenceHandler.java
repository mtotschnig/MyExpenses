package org.totschnig.myexpenses.util.licence;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.apache.commons.lang3.time.DateUtils;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
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
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class LicenceHandler {
  protected static final String LICENSE_STATUS_KEY = "licence_status";
  private static final String LICENSE_VALID_SINCE_KEY = "licence_valid_since";
  private static final String LICENSE_VALID_UNTIL_KEY = "licence_valid_until";
  public static final String TAG = "LicenceHandler";
  protected final MyApplication context;
  private final CrashHandler crashHandler;
  private boolean isSandbox = BuildConfig.DEBUG;

  private LicenceStatus licenceStatus;
  PreferenceObfuscator licenseStatusPrefs;
  CurrencyUnit currencyUnit;

  public LicenceHandler(MyApplication context, PreferenceObfuscator preferenceObfuscator, CrashHandler crashHandler) {
    this.context = context;
    this.licenseStatusPrefs = preferenceObfuscator;
    this.currencyUnit = CurrencyUnit.create("EUR", "€", 2);
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
      setLicenceStatus(licenseStatusPrefsString != null ? LicenceStatus.valueOf(licenseStatusPrefsString) : null);
    } catch (IllegalArgumentException e) {
      setLicenceStatus(null);
    }
  }

  public final void update() {
    Template.updateNewPlanEnabled();
    Account.updateNewAccountEnabled();
    GenericAccountService.updateAccountsIsSyncable(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      ShortcutHelper.configureSplitShortcut(context, isContribEnabled());
    }
    AbstractWidget.updateWidgets(context, TemplateWidget.class);
  }

  public void updateLicenceStatus(Licence licence) {
    if (licence == null || licence.getType() == null) {
      setLicenceStatus(null);
      licenseStatusPrefs.remove(LICENSE_STATUS_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
    } else {
      setLicenceStatus(licence.getType());
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

  @VisibleForTesting
  public void setLockState(boolean locked) {
    if (MyApplication.isInstrumentationTest()) {
      setLicenceStatus(locked ? null : LicenceStatus.PROFESSIONAL);
      update();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Nullable
  public String getFormattedPrice(Package aPackage) {
    return getFormattedPriceWithExtra(aPackage, false);
  }

  @Nullable
  private String getFormattedPriceWithExtra(Package aPackage, boolean withExtra) {
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
        aPackage.getFormattedPriceRaw(currencyUnit));
  }

  @NonNull
  public String getProLicenceStatus(Context context) {
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
    return new Package[]{Package.Professional_6, Package.Professional_18, Package.Professional_30};
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

  public OpenIabHelper getIabHelper(Context context) {
    return null;
  }

  public void registerSubscription(String sku) {
  }

  public void registerPurchase(boolean extended) {
  }

  public String getSkuForPackage(Package aPackage) {
    return null;
  }

  public String getCurrentSubscription() {
    return null;
  }

  public void maybeCancel() {
  }

  public void storeSkuDetails(Inventory inventory) {
  }

  @VisibleForTesting
  public @Nullable
  String findHighestValidSku(List<String> inventory) {
    return Stream.of(inventory)
        .filter(sku -> extractLicenceStatusFromSku(sku) != null)
        .max((o, o2) -> Utils.compare(extractLicenceStatusFromSku(o), extractLicenceStatusFromSku(o2), Enum::compareTo))
        .orElse(null);
  }

  public void registerInventory(Inventory inventory) {
    Timber.tag(TAG);
    Timber.i(Stream.of(inventory.getAllOwnedSkus()).collect(Collectors.joining(", ")));
    String sku = findHighestValidSku(inventory.getAllOwnedSkus());
    if (sku != null) {
      Purchase purchase = inventory.getPurchase(sku);
      handlePurchase(sku, purchase != null ? purchase.getOrderId() : null);
    } else {
      maybeCancel();
    }
  }

  /**
   * @param sku
   * @return which LicenceStatus an sku gives access to
   */
  @VisibleForTesting
  @Nullable
  public LicenceStatus extractLicenceStatusFromSku(@NonNull String sku) {
    if (sku.contains(LicenceStatus.PROFESSIONAL.toSkuType())) return LicenceStatus.PROFESSIONAL;
    if (sku.contains(LicenceStatus.EXTENDED.toSkuType())) return LicenceStatus.EXTENDED;
    if (sku.contains(LicenceStatus.CONTRIB.toSkuType())) return LicenceStatus.CONTRIB;
    return null;
  }

  @Nullable
  public LicenceStatus handlePurchase(@Nullable String sku, @Nullable String orderId) {
    LicenceStatus licenceStatus = sku != null ? extractLicenceStatusFromSku(sku) : null;
    if (licenceStatus != null) {
      switch (licenceStatus) {
        case CONTRIB:
          registerPurchase(false);
          break;
        case EXTENDED:
          registerPurchase(true);
          break;
        case PROFESSIONAL:
          registerSubscription(sku);
          break;
      }
    }
    return licenceStatus;
  }


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
      setLicenceStatus(LicenceStatus.EXTENDED);
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);licenseStatusPrefs.commit();
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

  protected void setLicenceStatus(@Nullable LicenceStatus licenceStatus) {
    this.licenceStatus = licenceStatus;
    crashHandler.putCustomData("Licence", licenceStatus != null ? licenceStatus.name() : "null");
  }

  @Nullable public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }
}