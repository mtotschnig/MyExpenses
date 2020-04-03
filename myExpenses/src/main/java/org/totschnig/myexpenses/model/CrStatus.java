package org.totschnig.myexpenses.model;

import android.graphics.Color;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public enum CrStatus {
  UNRECONCILED(Color.GRAY, ""), CLEARED(Color.BLUE, "*"), RECONCILED(Color.GREEN, "X"), VOID(Color.RED, "V");
  public int color;
  @NonNull
  public String symbol;

  CrStatus(int color, @NonNull String symbol) {
    this.color = color;
    this.symbol = symbol;
  }

  public static final String JOIN;

  static {
    JOIN = TextUtils.joinEnum(CrStatus.class);
  }

  public static CrStatus fromQifName(String qifName) {
    if (qifName == null)
      return UNRECONCILED;
    if (qifName.equals("*") || qifName.equalsIgnoreCase("C")) {
      return CLEARED;
    } else if (qifName.equalsIgnoreCase("X") || qifName.equalsIgnoreCase("R")) {
      return RECONCILED;
    } else {
      return UNRECONCILED;
    }
  }

  @StringRes
  public int toStringRes() {
    switch (this) {
      case CLEARED:
        return R.string.status_cleared;
      case RECONCILED:
        return R.string.status_reconciled;
      case UNRECONCILED:
        return R.string.status_uncreconciled;
      case VOID:
        return R.string.status_void;
    }
    return 0;
  }
}
