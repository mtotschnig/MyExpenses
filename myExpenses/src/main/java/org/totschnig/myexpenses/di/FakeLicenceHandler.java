package org.totschnig.myexpenses.di;

import android.content.Context;

import org.totschnig.myexpenses.util.LicenceHandler;

public class FakeLicenceHandler extends LicenceHandler {
  public void setLicenceStatus(LicenceStatus licenceStatus) {
    this.licenceStatus = licenceStatus;
  }

  private LicenceStatus licenceStatus = null;

  @Override
  public void init(Context ctx) {

  }

  @Override
  public boolean isContribEnabled() {
    return licenceStatus != null;
  }

  @Override
  public boolean isExtendedEnabled() {
    return licenceStatus == LicenceStatus.EXTENDED;
  }

}
