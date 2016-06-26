package org.totschnig.myexpenses.util;

import android.app.Application;

public class NoopAcraWrapper implements AcraWrapperIFace {

  @Override
  public void init(Application context) {}

  @Override
  public boolean isACRASenderServiceProcess() {
    return false;
  }
}
