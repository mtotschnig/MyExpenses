package org.totschnig.myexpenses.preference;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import com.android.calendar.CalendarContractCompat.Calendars;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.preference.ListPreference;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.AttributeSet;
import android.widget.Toast;

public class CalendarListPreference extends ListPreference {
  public CalendarListPreference(Context context) {
    super(context);
  }
  public CalendarListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  @Override
  protected void onPrepareDialogBuilder( AlertDialog.Builder builder ) {
    boolean localExists = false;
    Cursor selectionCursor;
    String value = getSharedPreferences().getString(getKey(), "-1");
    int selectedIndex = -1;
    String[] projection =
      new String[]{
            Calendars._ID,
            Calendars.ACCOUNT_NAME,
            Calendars.NAME,
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
    if (calCursor != null) {
      if (calCursor.moveToFirst()) {
        do {
          if (calCursor.getString(0).equals(value)) {
            selectedIndex = calCursor.getPosition();
          }
          if (calCursor.getString(1).equals(MyApplication.PLANNER_ACCOUNT_NAME)
              && calCursor.getString(2).equals(MyApplication.PLANNER_CALENDAR_NAME))
            localExists = true;
        } while (calCursor.moveToNext());
      }
      if (localExists)
        selectionCursor = calCursor;
      else {
        MatrixCursor extras = new MatrixCursor(new String[] {
            Calendars._ID,
            Calendars.ACCOUNT_NAME,
            Calendars.NAME,
            "full_name"});
        extras.addRow(new String[] {
            "-1", "","",
            getContext().getString(R.string.pref_planning_calendar_create_local) });
        selectionCursor = new MergeCursor(new Cursor[] {calCursor,extras});
      }
      builder.setSingleChoiceItems(
          new SimpleCursorAdapter(getContext(), android.R.layout.select_dialog_singlechoice,
              selectionCursor, new String[]{"full_name"}, new int[]{android.R.id.text1},0),
          selectedIndex,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              long itemId = ((AlertDialog) dialog).getListView().getItemIdAtPosition(which);
              if (itemId == -1) {
                boolean success = MyApplication.getInstance().createPlanner();
                Toast.makeText(
                    getContext(),
                    success ? R.string.planner_create_calendar_success : R.string.planner_create_calendar_failure,
                    Toast.LENGTH_LONG).show();
              }
              else if (callChangeListener(itemId)) {
                setValue(String.valueOf(itemId));
              }
                /*
                 * Clicking on an item simulates the positive button
                 * click, and dismisses the dialog.
                 */
                CalendarListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
          });
    } else {
      builder.setMessage("Calendar provider not available");
    }
    builder.setPositiveButton( null, null );
  }
  @Override
  protected void onDialogClosed(boolean positiveResult) {
    //nothing to do, value already stored
  }
}
