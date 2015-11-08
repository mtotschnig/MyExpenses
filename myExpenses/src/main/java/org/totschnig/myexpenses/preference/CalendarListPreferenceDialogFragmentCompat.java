package org.totschnig.myexpenses.preference;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.provider.DbUtils;

/**
 * Created by privat on 07.11.15.
 */
public class CalendarListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  @Override
  protected void onPrepareDialogBuilder( AlertDialog.Builder builder ) {
    final ListPreference preference = (ListPreference) getPreference();
    boolean localExists = false;
    Cursor selectionCursor;
    final String value = preference.getValue();
    int selectedIndex = -1;
    String[] projection =
        new String[]{
            CalendarContractCompat.Calendars._ID,
            CalendarContractCompat.Calendars.ACCOUNT_NAME,
            CalendarContractCompat.Calendars.ACCOUNT_TYPE,
            CalendarContractCompat.Calendars.NAME,
            "ifnull(" + CalendarContractCompat.Calendars.ACCOUNT_NAME + ",'') || ' / ' ||" +
                "ifnull(" + CalendarContractCompat.Calendars.CALENDAR_DISPLAY_NAME + ",'') AS full_name"
        };
    Cursor calCursor = null;
    try {
      calCursor = getContext().getContentResolver().
          query(CalendarContractCompat.Calendars.CONTENT_URI,
              projection,
              CalendarContractCompat.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContractCompat.Calendars.CAL_ACCESS_CONTRIBUTOR,
              null,
              CalendarContractCompat.Calendars._ID + " ASC");
    } catch (SecurityException e) {
      // android.permission.READ_CALENDAR or android.permission.WRITE_CALENDAR missing
    }
    if (calCursor != null) {
      if (calCursor.moveToFirst()) {
        do {
          if (calCursor.getString(0).equals(value)) {
            selectedIndex = calCursor.getPosition();
          }
          if (DbUtils.getString(calCursor, 1).equals(MyApplication.PLANNER_ACCOUNT_NAME)
              && DbUtils.getString(calCursor,2).equals(CalendarContractCompat.ACCOUNT_TYPE_LOCAL)
              && DbUtils.getString(calCursor,3).equals(MyApplication.PLANNER_CALENDAR_NAME))
            localExists = true;
        } while (calCursor.moveToNext());
      }
      if (localExists || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        selectionCursor = calCursor;
      } else {
        MatrixCursor extras = new MatrixCursor(new String[] {
            CalendarContractCompat.Calendars._ID,
            CalendarContractCompat.Calendars.ACCOUNT_NAME,
            CalendarContractCompat.Calendars.ACCOUNT_TYPE,
            CalendarContractCompat.Calendars.NAME,
            "full_name"});
        extras.addRow(new String[] {
            "-1", "","","",
            getContext().getString(R.string.pref_planning_calendar_create_local) });
        selectionCursor = new MergeCursor(new Cursor[] {calCursor,extras});
      }
      builder.setSingleChoiceItems(selectionCursor,selectedIndex,"full_name",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              long itemId = ((AlertDialog) dialog).getListView().getItemIdAtPosition(which);
              if (itemId == -1) {
                ((MyPreferenceActivity) getContext()).showDialog(R.id.PLANNER_SETUP_INFO_CREATE_NEW_WARNING_DIALOG);
              } else {
                if(preference.callChangeListener(itemId)) {
                  preference.setValue(String.valueOf(itemId));
                }
              }
              CalendarListPreferenceDialogFragmentCompat.this.onClick(dialog, -1);
              dialog.dismiss();
            }
          });
    } else {
      builder.setMessage("Calendar provider not available");
    }
    builder.setPositiveButton( null, null );
  }
  @Override
  public void onDialogClosed(boolean positiveResult) {
    if (positiveResult)
      ((MyPreferenceActivity) getContext()).onCalendarListPreferenceSet();
  }

  public static CalendarListPreferenceDialogFragmentCompat newInstance(Preference preference) {
    CalendarListPreferenceDialogFragmentCompat fragment = new CalendarListPreferenceDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString("key", preference.getKey());
    fragment.setArguments(bundle);
    return fragment;
  }
}
