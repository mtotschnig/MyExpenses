package org.totschnig.myexpenses.task;

import java.lang.ref.WeakReference;

import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
  private final WeakReference<ImageView> imageViewReference;
  private final WeakReference<AlertDialog> dialogWeekReference;
  private int thumbSize;

  public BitmapWorkerTask(ImageView imageView, int thumbSize) {
    // Use a WeakReference to ensure the ImageView can be garbage collected
    imageViewReference = new WeakReference<ImageView>(imageView);
    dialogWeekReference = null;
    this.thumbSize = thumbSize;
  }

  public BitmapWorkerTask(AlertDialog dialog, int thumbSize) {
    // Use a WeakReference to ensure the ImageView can be garbage collected
    imageViewReference = null;
    dialogWeekReference = new WeakReference<AlertDialog>(dialog);
    this.thumbSize = thumbSize;
  }

  // Decode image in background.
  @Override
  protected Bitmap doInBackground(Uri... params) {
    return ThumbnailUtils.extractThumbnail(
        Utils.decodeSampledBitmapFromUri(params[0], thumbSize, thumbSize),
        thumbSize, thumbSize);
  }

  // Once complete, see if ImageView is still around and set bitmap.
  @Override
  protected void onPostExecute(Bitmap bitmap) {
    if (bitmap != null) {
      if (imageViewReference != null) {
        final ImageView imageView = imageViewReference.get();
        if (imageView != null) {
          imageView.setImageBitmap(bitmap);
        }
      } else if (dialogWeekReference !=null) {
        final AlertDialog dialog = dialogWeekReference.get();
        if (dialog != null) {
          dialog.setIcon(new BitmapDrawable(dialog.getContext().getResources(),
              bitmap));
        }
      }
    }
  }
}