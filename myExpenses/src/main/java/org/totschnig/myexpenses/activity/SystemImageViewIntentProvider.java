package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.util.PictureDirHelper;

public class SystemImageViewIntentProvider implements ImageViewIntentProvider {
  public Intent getViewIntent(Context context, Uri pictureUri) {
    pictureUri = PictureDirHelper.ensureContentUri(pictureUri);
    Intent intent = instantiateIntent(pictureUri);
    intent.putExtra(Intent.EXTRA_STREAM, pictureUri);
    intent.setDataAndType(pictureUri, "image/jpeg");
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    return intent;
  }

  @NonNull
  protected Intent instantiateIntent(Uri pictureUri) {
    return new Intent(Intent.ACTION_VIEW, pictureUri);
  }
}
