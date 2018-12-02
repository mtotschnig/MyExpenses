package org.totschnig.myexpenses.viewmodel.data;

import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import org.totschnig.myexpenses.model.CurrencyEnum;

import java.io.Serializable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

@AutoValue
public abstract class Currency implements Serializable {
  public abstract String code();
  //should not count for equals
  private String displayName;
  final String displayName() {
    return displayName;
  }

  public static Currency create(@NonNull String code) {
    Currency currency = new AutoValue_Currency(code);
    currency.displayName = findDisplayName(code);
    return currency;
  }

  public static Currency create(Cursor cursor) {
    final String code = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CODE));
    Currency currency = new AutoValue_Currency(code);
    currency.displayName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL));
    if (currency.displayName == null) {
      currency.displayName = findDisplayName(code);
    }
    return currency;
  }

  private static String findDisplayName(String code) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      try {
        return java.util.Currency.getInstance(code).getDisplayName();
      } catch (IllegalArgumentException ignored) {}
    }
    try {
      return CurrencyEnum.valueOf(code).getDescription();
    } catch (IllegalArgumentException ignored) {}
    return code;
  }

  public int sortClass() {
    switch (code()) {
      case "XXX":
        return 3;
      case "XAU":
      case "XPD":
      case "XPT":
      case "XAG":
        return 2;
      default:
        return 1;
    }
  }

  @Override
  public String toString() {
    return displayName();
  }
}
