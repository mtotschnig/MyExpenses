package org.totschnig.myexpenses.util.licence;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.annimon.stream.Stream;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.apache.commons.lang3.time.DateUtils;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Preconditions;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.util.Currency;
import java.util.Date;
import java.util.List;

public class LicenceHandler {
  private static final String LICENSE_STATUS_KEY = "licence_status";
  private static final String LICENSE_VALID_SINCE_KEY = "licence_valid_since";
  private static final String LICENSE_VALID_UNTIL_KEY = "licence_valid_until";
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();
  public static LicenceStatus EXTENDED = HAS_EXTENDED ? LicenceStatus.EXTENDED : LicenceStatus.CONTRIB;
  protected final Context context;

  public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }

  protected LicenceStatus licenceStatus;
  PreferenceObfuscator licenseStatusPrefs;

  protected LicenceHandler(Context context, PreferenceObfuscator preferenceObfuscator) {
    this.context = context;
    this.licenseStatusPrefs = preferenceObfuscator;
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
      licenceStatus = licenseStatusPrefsString != null ? LicenceStatus.valueOf(licenseStatusPrefsString) : null;
    } catch (IllegalArgumentException e) {
      licenceStatus = null;
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
      licenceStatus = null;
      licenseStatusPrefs.remove(LICENSE_STATUS_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
    } else {
      licenceStatus = licence.getType();
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      if (licence.getValidSince() != null) {
        licenseStatusPrefs.putString(LICENSE_VALID_SINCE_KEY, String.valueOf(licence.getValidSince().getTime()));
      }
      if (licence.getValidUntil() != null) {
        licenseStatusPrefs.putString(LICENSE_VALID_UNTIL_KEY, String.valueOf(licence.getValidUntil().getTime()));
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
      licenceStatus = locked ? null : LicenceStatus.CONTRIB;
      update();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Nullable
  public String getFormattedPrice(Package aPackage) {
    return aPackage.getFormattedPrice(context, aPackage.getFormattedPriceRaw());
  }

  public String getExtendOrSwitchMessage(Package aPackage) {
    Preconditions.checkArgument(aPackage.isProfessional());
    Date extendedDate = DateUtils.addMonths(getValidUntilDate(), aPackage.getDuration());
    return context.getString(R.string.extend_until,
        Utils.getDateFormatSafe(context).format(extendedDate),
        aPackage.getFormattedPriceRaw());
  }

  @NonNull
  public String getProLicenceStatus(Context context) {
    return context.getString(R.string.valid_until, Utils.getDateFormatSafe(this.context).format(getValidUntilDate()));
  }

  @NonNull
  private Date getValidUntilDate() {
    return new Date(Long.parseLong(
        licenseStatusPrefs.getString(LICENSE_VALID_UNTIL_KEY, "0")));
  }

  public boolean hasLegacyLicence() {
    return false;
  }

  public Package[] getProPackages() {
    return new Package[]{Package.Professional_6, Package.Professional_36};
  }

  @Nullable
  public String getExtendedUpgradeGoodieMessage(Package selectedPackage) {
    return context.getString(R.string.extended_upgrade_goodie_github, 3);
  }

  public String getProfessionalPriceShortInfo() {
    String minimumProfessionalMonthlyPrice = getMinimumProfessionalMonthlyPrice();
    if (minimumProfessionalMonthlyPrice != null) {
      return context.getString(R.string.professionalPriceShortInfo, minimumProfessionalMonthlyPrice);
    } else {
      return getProfessionalPriceFallBack();
    }
  }

  protected String getProfessionalPriceFallBack() {
    return null;
  }

  protected String getMinimumProfessionalMonthlyPrice() {
    return CurrencyFormatter.instance().formatCurrency(
        new Money(Currency.getInstance("EUR"), (long) Math.ceil((double) Package.Professional_36.getDefaultPrice() / 36)));
  }

  @Nullable
  public Package[] getProPackagesForExtendOrSwitch() {
    return getProPackages();
  }

  public String getProLicenceAction(Context context) {
    return context.getString(R.string.extend_validity);
  }

  public String getPayLoad() {
    return null;
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
  public  @Nullable
  String findHighestValidSku(List<String> inventory) {
    return Stream.of(inventory)
        .filter(sku -> extractLicenceStatusFromSku(sku) != null)
        .max((o, o2) -> Utils.compare(extractLicenceStatusFromSku(o), extractLicenceStatusFromSku(o2), Enum::compareTo))
        .orElse(null);
  }

  public void registerInventory(Inventory inventory) {
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
  public LicenceStatus handlePurchase(@Nullable String  sku, @Nullable String orderId) {
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
}