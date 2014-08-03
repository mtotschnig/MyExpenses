package org.totschnig.myexpenses.provider.filter;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.Parcel;
import android.os.Parcelable;

public class CrStatusCriteria extends Criteria {
  public CrStatusCriteria(int searchIndex) {
    super(DatabaseConstants.KEY_CR_STATUS, WhereFilter.Operation.EQ,
        CrStatus.values()[searchIndex].name());
    this.title = MyApplication.getInstance().getString(R.string.status);
  }
  public CrStatusCriteria(Parcel in) {
    super(in);
  }
  @Override
  public String prettyPrint() {
    return prettyPrintInternal(CrStatus.valueOf(getStringValue()).toString());
  }
  public static final Parcelable.Creator<CrStatusCriteria> CREATOR = new Parcelable.Creator<CrStatusCriteria>() {
    public CrStatusCriteria createFromParcel(Parcel in) {
        return new CrStatusCriteria(in);
    }

    public CrStatusCriteria[] newArray(int size) {
        return new CrStatusCriteria[size];
    }
};
}
