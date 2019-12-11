package org.totschnig.myexpenses.viewmodel;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.Nullable;

class DatabaseHandler extends AsyncQueryHandler {
  interface UpdateListener {
    void onUpdateComplete(int token, int resultCount);
  }

  interface InsertListener {
    void onInsertComplete(int token, @Nullable Uri uri);
  }

  interface DeleteListener {
    void onDeleteComplete(int token, int result);
  }

  public DatabaseHandler(ContentResolver cr) {
    super(cr);
  }

  @Override
  protected void onUpdateComplete(int token, Object cookie, int result) {
    if (cookie instanceof  UpdateListener) {
      ((UpdateListener) cookie).onUpdateComplete(token, result);
    }
  }

  @Override
  protected void onInsertComplete(int token, Object cookie, Uri uri) {
    if (cookie instanceof InsertListener) {
      ((InsertListener) cookie).onInsertComplete(token, uri);
    }
  }

  @Override
  protected void onDeleteComplete(int token, Object cookie, int result) {
    if (cookie instanceof DeleteListener) {
      ((DeleteListener) cookie).onDeleteComplete(token, result);
    }
  }
}
