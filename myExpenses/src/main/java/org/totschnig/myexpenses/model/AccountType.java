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
    switch (this) {
      case CASH:
        return R.string.account_type_cash_plural;
      case BANK:
        return R.string.account_type_bank_plural;
      case CCARD:
        return R.string.account_type_ccard_plural;
      case ASSET:
        return R.string.account_type_asset_plural;
      case LIABILITY:
        return R.string.account_type_liability_plural;
      default:
        return 0;
    }
  }

  public String toQifName() {
    switch (this) {
      case CASH:
        return "Cash";
      case BANK:
        return "Bank";
      case CCARD:
        return "CCard";
      case ASSET:
        return "Oth A";
      case LIABILITY:
        return "Oth L";
    }
    return "";
  }

  public static AccountType fromQifName(String qifName) {
    switch (qifName) {
      case "Oth L":
        return LIABILITY;
      case "Oth A":
        return ASSET;
      case "CCard":
        return CCARD;
      case "Cash":
        return CASH;
      default:
        return BANK;
    }
  }

  public static String sqlOrderExpression() {
    String result = "CASE " + KEY_TYPE;
    for (AccountType type : AccountType.values()) {
      result += " WHEN '" + type.name() + "' THEN " + type.getSortOrder();
    }
    result += " ELSE -1 END AS " + KEY_SORT_KEY_TYPE;
    return result;
  }

  private String getSortOrder() {
    switch (this) {
      case CASH:
        return "0";
      case BANK:
        return "1";
      case CCARD:
        return "2";
      case ASSET:
        return "3";
      case LIABILITY:
        return "4";
    }
    return "-1";
  }

  static {
    JOIN = TextUtils.joinEnum(AccountType.class);
  }

  @StringRes
  public int toStringRes() {
    switch (this) {
      case CASH:
        return R.string.account_type_cash;
      case BANK:
        return R.string.account_type_bank;
      case CCARD:
        return R.string.account_type_ccard;
      case ASSET:
        return R.string.account_type_asset;
      case LIABILITY:
        return R.string.account_type_liability;
    }
    return 0;
  }
}
