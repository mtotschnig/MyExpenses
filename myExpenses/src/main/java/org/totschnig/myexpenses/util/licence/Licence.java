package org.totschnig.myexpenses.util.licence;

import com.google.gson.annotations.SerializedName;

import org.totschnig.myexpenses.util.LicenceHandler;

import java.util.Date;

public class Licence {
  public Date getValidSince() {
    return validSince;
  }

  public Date getValidUntil() {
    return validUntil;
  }

  public LicenceHandler.LicenceStatus getType() {
    return type;
  }


  @SerializedName("valid_since")
  Date validSince;
  @SerializedName("valid_until")
  Date validUntil;
  @SerializedName("type")
  LicenceHandler.LicenceStatus type;
}
