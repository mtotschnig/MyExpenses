package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

public interface AdHandlerFactory {
  AdHandler create(ViewGroup adContainer);
}
