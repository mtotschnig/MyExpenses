package org.totschnig.myexpenses.util.licence;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.LocalDate;

public class Licence {
  public LocalDate getValidSince() {
    return validSince;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public LicenceStatus getType() {
    return type;
  }


  @SerializedName("valid_since")
  LocalDate validSince;
  @SerializedName("valid_until")
  LocalDate validUntil;
  @SerializedName("type")
  LicenceStatus type;
}
