package org.totschnig.myexpenses.fragment;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.calendar.CalendarContractCompat;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CellView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * Created by michaeltotschnig on 04.04.16.
 */
public class PlanMonthFragment extends CaldroidFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private LoaderManager mManager;
  public static final int EVENTS_CURSOR = 1;


  public static PlanMonthFragment newInstance(String title, long planId, int color) {
    PlanMonthFragment f = new PlanMonthFragment();
    Bundle args = new Bundle();
    args.putInt(CaldroidFragment.DIALOG_TITLE_CUSTOM_VIEW, R.layout.calendar_title);
    args.putString(CaldroidFragment.DIALOG_TITLE, title);
    args.putInt(CaldroidFragment.THEME_RESOURCE,
        MyApplication.getThemeType().equals(MyApplication.ThemeType.dark) ?
            R.style.CaldroidCustomDark : R.style.CaldroidCustom);
    args.putLong(DatabaseConstants.KEY_PLANID, planId);
    args.putInt(DatabaseConstants.KEY_COLOR, color);
    f.setArguments(args);
    return f;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    mManager = getLoaderManager();
    mManager.initLoader(EVENTS_CURSOR, null, this);
    return view;
  }

  @Override
  public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
    return new CaldroidSampleCustomAdapter(getActivity(), month, year, getCaldroidData(), extraData);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    // Construct the query with the desired date range.
    Uri.Builder builder = CalendarContractCompat.Instances.CONTENT_URI.buildUpon();
    long start = DateTime.now(TimeZone.getDefault()).getStartOfMonth()
        .getMilliseconds(TimeZone.getDefault());
    long end = DateTime.now(TimeZone.getDefault()).getEndOfMonth()
        .getMilliseconds(TimeZone.getDefault());
    ContentUris.appendId(builder, start);
    ContentUris.appendId(builder, end);
    return new CursorLoader(
        getActivity(),
        builder.build(),
        new String[]{
            CalendarContractCompat.Instances._ID,
            CalendarContractCompat.Instances.BEGIN
        },
        CalendarContractCompat.Instances.EVENT_ID + " = ?",
        new String[]{String.valueOf(getArguments().getLong(DatabaseConstants.KEY_PLANID))},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    Calendar calendar = Calendar.getInstance();
    data.moveToFirst();
    ColorDrawable colorDrawable = new ColorDrawable(
        getArguments().getInt(DatabaseConstants.KEY_COLOR));
    while (!data.isAfterLast()) {
      calendar.setTimeInMillis(data.getLong(1));
      setBackgroundDrawableForDate(
          colorDrawable,
          calendar.getTime());
      data.moveToNext();
    }
    refreshView();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {

  }

  private class CaldroidSampleCustomAdapter extends CaldroidGridAdapter {

    public CaldroidSampleCustomAdapter(Context context, int month, int year,
                                       Map<String, Object> caldroidData,
                                       Map<String, Object> extraData) {
      super(context, month, year, caldroidData, extraData);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater) context
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View framelayout;

      // For reuse
      if (convertView == null) {
        framelayout = inflater.inflate(R.layout.plan_calendar_cell, null);
      } else {
        framelayout = convertView;
      }

      CellView cell = (CellView) framelayout.findViewById(R.id.cell);
      ImageView state = (ImageView) framelayout.findViewById(R.id.state);

      customizeTextView(position, cell);

      state.setImageResource(R.drawable.ic_stat_applied);

      return framelayout;
    }
  }
}
