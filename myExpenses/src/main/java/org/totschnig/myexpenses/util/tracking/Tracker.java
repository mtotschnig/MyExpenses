package org.totschnig.myexpenses.util.tracking;

import android.content.Context;
import android.os.Bundle;

public interface Tracker {
  void init(Context context);
  void logEvent(String eventName, Bundle params);
}
