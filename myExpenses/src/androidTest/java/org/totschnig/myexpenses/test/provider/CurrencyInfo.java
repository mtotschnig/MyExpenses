package org.totschnig.myexpenses.test.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.provider.DatabaseConstants;

public class CurrencyInfo {
  String label;
  String code;

  public CurrencyInfo(String label, String code) {
    this.label = label;
    this.code = code;
  }
  public ContentValues getContentValues() {
    ContentValues v = new ContentValues();

    v.put(DatabaseConstants.KEY_LABEL, label);
    v.put(DatabaseConstants.KEY_CODE, code);
    return v;
  }

}
