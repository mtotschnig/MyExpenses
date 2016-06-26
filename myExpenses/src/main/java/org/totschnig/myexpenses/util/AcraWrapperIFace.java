package org.totschnig.myexpenses.util;

import android.app.Application;

public interface AcraWrapperIFace {
  void init(Application context);
  boolean isACRASenderServiceProcess();

  void reportToAcraWithDbSchema(Exception e);

  void reportToAcra(Exception e, String key, String data);

  void reportToAcra(Exception e);
}
