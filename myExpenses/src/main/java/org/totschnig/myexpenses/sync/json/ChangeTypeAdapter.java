package org.totschnig.myexpenses.sync.json;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

import android.content.ContentValues;
import android.database.Cursor;

import com.gabrielittner.auto.value.cursor.ColumnTypeAdapter;

public class ChangeTypeAdapter implements ColumnTypeAdapter<TransactionChange.Type> {
  @Override
  public TransactionChange.Type fromCursor(Cursor cursor, String columnName) {
    return TransactionChange.Type.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)));
  }

  @Override
  public void toContentValues(ContentValues values, String columnName, TransactionChange.Type value) {
    //not used
  }
}
