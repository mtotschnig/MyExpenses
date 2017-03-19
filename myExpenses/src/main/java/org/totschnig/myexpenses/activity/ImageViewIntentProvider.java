package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public interface ImageViewIntentProvider {
  Intent getViewIntent(Context context, Uri pictureUri);
}
