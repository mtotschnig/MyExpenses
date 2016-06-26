package org.totschnig.myexpenses.util;

public interface LicenceHandlerIFace {
  boolean isContribEnabled();

  boolean isExtendedEnabled();

  void resetContribEnabled();

  LicenceStatus verifyLicenceKey(String tag);

  void setContribEnabled(LicenceStatus licenceStatus);

  public enum LicenceStatus {
    CONTRIB, EXTENDED
  }

}
