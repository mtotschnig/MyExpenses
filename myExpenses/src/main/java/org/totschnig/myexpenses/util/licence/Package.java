package org.totschnig.myexpenses.util.licence;

import android.content.Context;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Preconditions;

import java.util.Currency;

public enum Package {
  Contrib(350), Upgrade(300), Extended(500), Professional_1(100), Professional_6(500), Professional_12(900), Professional_36(2000), Professional_Amazon(900), Professional_Blackberry(2000);

  public long getDefaultPrice() {
    return defaultPrice;
  }

  private final long defaultPrice;

  Package(long price) {
    this.defaultPrice = price;
  }

  public boolean isProfessional() {
    return name().startsWith("Professional");
  }

  public String getFormattedPrice(Context context, String formatted) {
    return isProfessional() ? formatWithDuration(context, formatted) : formatted;
  }

  public String getFormattedPriceRaw() {
    return CurrencyFormatter.instance().formatCurrency(
        new Money(Currency.getInstance("EUR"), getDefaultPrice()));
  }

  @NonNull
  String formatWithDuration(Context context, String formattedPrice) {
    Preconditions.checkState(isProfessional());
    String duration = extractDuration();
    String formattedDuration;
    String format = "%s (%s)";
    switch (duration) {
      case "1":
        formattedDuration = context.getString(R.string.monthly);
        break;
      case "12":
        formattedDuration = context.getString(R.string.yearly_plain);
        break;
      default:
        format= "%s / %s";
        formattedDuration= context.getString(R.string.n_months, duration);
    }
    return String.format(format, formattedPrice, formattedDuration);
  }

  int getDuration() {
    return Integer.parseInt(extractDuration());
  }

  @NonNull
  private String extractDuration() {
    return name().substring(name().lastIndexOf("_") + 1);
  }

  public String getButtonLabel(Context context) {
    int resId;
    switch (this) {
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
    return String.format("%s (%s)", context.getString(resId), getFormattedPrice(context, getFormattedPriceRaw()));
  }
}
