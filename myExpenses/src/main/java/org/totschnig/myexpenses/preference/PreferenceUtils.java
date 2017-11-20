package org.totschnig.myexpenses.preference;

public class PreferenceUtils {
  private PreferenceUtils() {
  }

  public static boolean shouldStartAutoFill() {
    return PrefKey.AUTO_FILL_AMOUNT.getBoolean(false) ||
        PrefKey.AUTO_FILL_CATEGORY.getBoolean(false) ||
        PrefKey.AUTO_FILL_COMMENT.getBoolean(false) ||
        PrefKey.AUTO_FILL_METHOD.getBoolean(false) ||
        !PrefKey.AUTO_FILL_ACCOUNT.getString("never").equals("never");
  }

  public static void enableAutoFill() {
    PrefKey.AUTO_FILL_AMOUNT.putBoolean(true);
    PrefKey.AUTO_FILL_CATEGORY.putBoolean(true);
    PrefKey.AUTO_FILL_COMMENT.putBoolean(true);
    PrefKey.AUTO_FILL_METHOD.putBoolean(true);
    PrefKey.AUTO_FILL_ACCOUNT.putString("aggregate");
  }

  public static void disableAutoFill() {
    PrefKey.AUTO_FILL_AMOUNT.putBoolean(false);
    PrefKey.AUTO_FILL_CATEGORY.putBoolean(false);
    PrefKey.AUTO_FILL_COMMENT.putBoolean(false);
    PrefKey.AUTO_FILL_METHOD.putBoolean(false);
    PrefKey.AUTO_FILL_ACCOUNT.putString("never");
  }
}
