package org.totschnig.myexpenses.util.licence;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Licence {
  public Date getValidSince() {
    return validSince;
  }

  public Date getValidUntil() {
    return validUntil;
  }

  public LicenceStatus getType() {
    return type;
  }


  @SerializedName("valid_since")
  Date validSince;
  @SerializedName("valid_until")
  Date validUntil;
  @SerializedName("type")
  LicenceStatus type;
}
