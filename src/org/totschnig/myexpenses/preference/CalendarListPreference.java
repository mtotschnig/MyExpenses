package org.totschnig.myexpenses.preference;

import com.android.calendar.CalendarContractCompat.Calendars;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.preference.ListPreference;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.AttributeSet;

public class CalendarListPreference extends ListPreference {
  public CalendarListPreference(Context context) {
    super(context);
  }
  public CalendarListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  @Override
  protected void onPrepareDialogBuilder( AlertDialog.Builder builder ) {
    String value = getSharedPreferences().getString(getKey(), "-1");
    int selectedIndex = -1;
    String[] projection =
      new String[]{
            Calendars._ID,
            Calendars.ACCOUNT_NAME + " || ' / ' ||" +
            Calendars.CALENDAR_DISPLAY_NAME + " AS full_name"
    };
    Cursor calCursor =
        getContext().getContentResolver().
            query(Calendars.CONTENT_URI,
                projection,
                Calendars.CALENDAR_ACCESS_LEVEL + " >= " + Calendars.CAL_ACCESS_CONTRIBUTOR,
                null,
                Calendars._ID + " ASC");
    if (calCursor != null && calCursor.moveToFirst()) {
      do {
        if (calCursor.getString(0).equals(value)) {
          selectedIndex = calCursor.getPosition();
          break;
        }
      } while (calCursor.moveToNext());
    }

    //builder.setCursor(calCursor, this, "full_name");
    builder.setSingleChoiceItems(
        new SimpleCursorAdapter(getContext(), android.R.layout.select_dialog_singlechoice,
            calCursor, new String[]{"full_name"}, new int[]{android.R.id.text1},0),
        selectedIndex,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            String itemId = String.valueOf(((AlertDialog) dialog).getListView().getItemIdAtPosition(which));
            if (callChangeListener(itemId)) {
              setValue(itemId);
            }
              /*
               * Clicking on an item simulates the positive button
               * click, and dismisses the dialog.
               */
              CalendarListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
              dialog.dismiss();
          }
        });
    builder.setPositiveButton( null, null );
  }
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    //nothing to do, value already stored
  }
}
