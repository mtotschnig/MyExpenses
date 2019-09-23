package org.totschnig.myexpenses.viewmodel;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.net.Uri;

class DatabaseHandler extends AsyncQueryHandler {
  interface UpdateListener {
    void onUpdateComplete(int token, int result);
  }

  interface InsertListener {
    void onInsertComplete(int token, boolean success);
  }

  interface DeleteListener {
    void onDeleteComplete(int token, boolean success);
  }

  public DatabaseHandler(ContentResolver cr) {
    super(cr);
  }

  @Override
  protected void onUpdateComplete(int token, Object cookie, int result) {
    ((UpdateListener) cookie).onUpdateComplete(token, result);
  }

  @Override
  protected void onInsertComplete(int token, Object cookie, Uri uri) {
    ((InsertListener) cookie).onInsertComplete(token, uri != null);
  }

  @Override
  protected void onDeleteComplete(int token, Object cookie, int result) {
    ((DeleteListener) cookie).onDeleteComplete(token, result == 1);
  }
}
