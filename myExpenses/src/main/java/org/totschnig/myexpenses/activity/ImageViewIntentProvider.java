package org.totschnig.myexpenses.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import androidx.annotation.NonNull;

public interface ImageViewIntentProvider {
  Intent getViewIntent(Context context, Uri pictureUri);

  default void startViewIntent(Activity activity, @NonNull Uri pictureUri) {
    try {
      activity.startActivity(getViewIntent(activity, pictureUri));
    } catch (SecurityException e) {
      CrashHandler.report(e, "pictureUri", pictureUri.toString());
    }
  }
}
