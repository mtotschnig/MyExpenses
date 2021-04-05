package org.totschnig.myexpenses.util.licence;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.ContribFeature;

import java.util.Locale;

import androidx.annotation.Nullable;

public enum LicenceStatus {
  CONTRIB(R.string.contrib_key), EXTENDED(R.string.extended_key), EXTENDED_FALLBACK(R.string.extended_key), PROFESSIONAL(R.string.professional_key) {
    @Override
    public boolean isUpgradeable() {
      return false;
    }
  };

  private final int resId;

  LicenceStatus(int resId) {
    this.resId = resId;
  }

  public int getResId() {
    return resId;
  }

  public boolean greaterOrEqual(@Nullable LicenceStatus other) {
    return other == null || compareTo(other) >= 0;
  }

  public boolean covers(ContribFeature contribFeature) {
    if (contribFeature == null) return true;
    return greaterOrEqual(contribFeature.getLicenceStatus());
  }

  public boolean isUpgradeable() {
    return true;
  }

  /**
   * for historical reasons, skus for Contrib used "premium"
   */
  public String toSkuType() {
    if (this == LicenceStatus.CONTRIB) {
      return "premium";
    }
    return name().toLowerCase(Locale.ROOT);
  }
}
