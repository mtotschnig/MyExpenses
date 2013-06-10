package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.MyApplication;

import android.content.ContentResolver;

public class Model {
  private static ContentResolver cr;

  public static ContentResolver cr() {
    return cr != null ? cr : MyApplication.getInstance().getContentResolver();
  }

  public static void setContentResolver(ContentResolver crIn) {
    cr = crIn;
  }
}
