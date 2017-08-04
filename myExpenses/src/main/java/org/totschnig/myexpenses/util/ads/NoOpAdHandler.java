package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

public class NoOpAdHandler extends AdHandler {
  public NoOpAdHandler(ViewGroup adContainer) {
    super(adContainer);
  }

  public void init() {
    hide();
  }
}
