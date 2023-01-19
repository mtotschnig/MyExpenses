package org.totschnig.myexpenses.viewmodel.data;

import android.database.Cursor;

import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

//TODO convert to kotlin data class
@AutoValue
public abstract class PaymentMethod {
  @ColumnName(KEY_ROWID)
  public abstract long id();
  @ColumnName(KEY_LABEL)
  public abstract String label();
  @ColumnName(KEY_IS_NUMBERED)
  public abstract boolean isNumbered();

  public static PaymentMethod create(Cursor cursor) {
    return AutoValue_PaymentMethod.createFromCursor(cursor);
  }

  @Override
  public String toString() {
    return label();
  }
}
