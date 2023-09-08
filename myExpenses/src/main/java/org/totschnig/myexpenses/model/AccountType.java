package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.TextUtils;

import androidx.annotation.StringRes;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public enum AccountType {
  CASH, BANK, CCARD, ASSET, LIABILITY;
  public static final String JOIN;

  public int toStringResPlural() {
    return switch (this) {
      case CASH -> R.string.account_type_cash_plural;
      case BANK -> R.string.account_type_bank_plural;
      case CCARD -> R.string.account_type_ccard_plural;
      case ASSET -> R.string.account_type_asset_plural;
      case LIABILITY -> R.string.account_type_liability_plural;
    };
  }

  public String toQifName() {
    return switch (this) {
      case CASH -> "Cash";
      case BANK -> "Bank";
      case CCARD -> "CCard";
      case ASSET -> "Oth A";
      case LIABILITY -> "Oth L";
    };
  }

  public static AccountType fromQifName(String qifName) {
    return switch (qifName) {
      case "Oth L" -> LIABILITY;
      case "Oth A" -> ASSET;
      case "CCard" -> CCARD;
      case "Cash" -> CASH;
      default -> BANK;
    };
  }

  public static String sqlOrderExpression() {
    StringBuilder result = new StringBuilder("CASE " + KEY_TYPE);
    for (AccountType type : AccountType.values()) {
      result.append(" WHEN '").append(type.name()).append("' THEN ").append(type.getSortOrder());
    }
    result.append(" ELSE -1 END AS " + KEY_SORT_KEY_TYPE);
    return result.toString();
  }

  private String getSortOrder() {
    return switch (this) {
      case CASH -> "0";
      case BANK -> "1";
      case CCARD -> "2";
      case ASSET -> "3";
      case LIABILITY -> "4";
    };
  }

  static {
    JOIN = TextUtils.joinEnum(AccountType.class);
  }

  @StringRes
  public int toStringRes() {
    return switch (this) {
      case CASH -> R.string.account_type_cash;
      case BANK -> R.string.account_type_bank;
      case CCARD -> R.string.account_type_ccard;
      case ASSET -> R.string.account_type_asset;
      case LIABILITY -> R.string.account_type_liability;
    };
  }
}
