package org.totschnig.myexpenses.task;

import java.lang.ref.WeakReference;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;

public class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
  private final WeakReference<ImageView> imageViewReference;
  private final WeakReference<AlertDialog> dialogWeekReference;
  private int thumbSize;

  public Uri getData() {
    return data;
  }

  private Uri data;

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
    data = params[0];
    Bitmap source = Utils.decodeSampledBitmapFromUri(data, thumbSize, thumbSize);
    if (source==null) {
      return null;
    }
    return ThumbnailUtils.extractThumbnail(
        source,
        thumbSize, thumbSize);
  }

  // Once complete, see if ImageView is still around and set bitmap.
  @Override
  protected void onPostExecute(Bitmap bitmap) {
    if (isCancelled()) {
      return;
    }

    if (imageViewReference != null) {
      final ImageView imageView = imageViewReference.get();
      if (imageView != null) {
        if (imageView.getDrawable() instanceof AsyncDrawable && this != getBitmapWorkerTask(imageView))
          return;
        if (bitmap!=null)
          imageView.setImageBitmap(bitmap);
        else
          Toast.makeText(imageView.getContext(),"Error extracting bitmap",Toast.LENGTH_LONG);
      }
    } else if (dialogWeekReference !=null) {
      final AlertDialog dialog = dialogWeekReference.get();
      if (dialog != null) {
        if (bitmap!=null)
          dialog.setIcon(new BitmapDrawable(dialog.getContext().getResources(),
              bitmap));
        else
          Toast.makeText(dialog.getContext(),"Error extracting bitmap",Toast.LENGTH_LONG);
      }
    }
  }
  public static class AsyncDrawable extends BitmapDrawable {
    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

    public AsyncDrawable(Resources res, Bitmap bitmap,
                         BitmapWorkerTask bitmapWorkerTask) {
      super(res, bitmap);
      bitmapWorkerTaskReference =
          new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
    }

    public BitmapWorkerTask getBitmapWorkerTask() {
      return bitmapWorkerTaskReference.get();
    }
  }
  public static boolean cancelPotentialWork(Uri data, ImageView imageView) {
    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

    if (bitmapWorkerTask != null) {
      final Uri bitmapData = bitmapWorkerTask.getData();
      // If bitmapData is not yet set or it differs from the new data
      if (bitmapData == null || bitmapData != data) {
        // Cancel previous task
        bitmapWorkerTask.cancel(true);
      } else {
        // The same work is already in progress
        return false;
      }
    }
    // No task associated with the ImageView, or an existing task was cancelled
    return true;
  }
  private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
    if (imageView != null) {
      final Drawable drawable = imageView.getDrawable();
      if (drawable instanceof AsyncDrawable) {
        final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
        return asyncDrawable.getBitmapWorkerTask();
      }
    }
    return null;
  }
}