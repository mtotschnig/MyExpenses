package org.totschnig.myexpenses.util;

import android.app.Application;

public interface AcraWrapperIFace {
  void init(Application context);
  boolean isACRASenderServiceProcess();
}
