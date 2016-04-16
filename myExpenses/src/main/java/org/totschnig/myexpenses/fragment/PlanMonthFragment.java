package org.totschnig.myexpenses.fragment;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.calendar.CalendarContractCompat;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CaldroidListener;
import com.roomorama.caldroid.CalendarHelper;
import com.roomorama.caldroid.CellView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

public class PlanMonthFragment extends CaldroidFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static boolean darkThemeSelected;
  private LoaderManager mManager;
  public static final int INSTANCES_CURSOR = 1;
  public static final int INSTANCE_STATUS_CURSOR = 2;

  private Map<DateTime, Long> dateTime2InstanceMap = new HashMap<>();
  private LongSparseArray<Long> instance2TransactionMap = new LongSparseArray<>();

  public static PlanMonthFragment newInstance(String title, long templateId, long planId, int color) {
    PlanMonthFragment f = new PlanMonthFragment();
    Bundle args = new Bundle();
    args.putInt(CaldroidFragment.DIALOG_TITLE_CUSTOM_VIEW, R.layout.calendar_title);
    args.putString(CaldroidFragment.DIALOG_TITLE, title);
    darkThemeSelected = MyApplication.getThemeType().equals(MyApplication.ThemeType.dark);
    args.putInt(CaldroidFragment.THEME_RESOURCE,
        darkThemeSelected ?
            R.style.CaldroidCustomDark : R.style.CaldroidCustom);
    args.putLong(DatabaseConstants.KEY_PLANID, planId);
    args.putInt(DatabaseConstants.KEY_COLOR, color);
    args.putLong(DatabaseConstants.KEY_ROWID, templateId);
    f.setArguments(args);
    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCaldroidListener(new CaldroidListener() {
      @Override
      public void onSelectDate(Date date, View view) {
        //not our concern
      }

      @Override
      public void onChangeMonth(int month, int year) {
        requireLoader(INSTANCES_CURSOR);
      }
    });
  }

  private void requireLoader(int loaderId) {
    if (mManager.getLoader(loaderId) != null && !mManager.getLoader(loaderId).isReset()) {
      mManager.restartLoader(loaderId, new Bundle(), this);
    } else {
      mManager.initLoader(loaderId, new Bundle(), this);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mManager = getLoaderManager();
    View view = super.onCreateView(inflater, container, savedInstanceState);
    requireLoader(INSTANCE_STATUS_CURSOR);
    return view;
  }


  @Override
  public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
    return new CaldroidCustomAdapter(getActivity(), month, year, getCaldroidData(), extraData);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case INSTANCES_CURSOR:
        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContractCompat.Instances.CONTENT_URI.buildUpon();
        DateTime startOfMonth = new DateTime(year, month, 1, 0, 0, 0, 0);
        long start = startOfMonth.minusDays(7)
            .getMilliseconds(TimeZone.getDefault());
        long end = startOfMonth.getEndOfMonth().plusDays(7)
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
            String.format(CalendarContractCompat.Instances.EVENT_ID + " = %d",
                getArguments().getLong(DatabaseConstants.KEY_PLANID)),
            null,
            null);
      case INSTANCE_STATUS_CURSOR:
        return new CursorLoader(
            getActivity(),
            TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            new String[]{
                KEY_TEMPLATEID,
                KEY_INSTANCEID,
                KEY_TRANSACTIONID
            },
            KEY_TEMPLATEID + " = ?",
            new String[]{String.valueOf(getArguments().getLong(KEY_ROWID))},
            null);
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    switch (loader.getId()) {
      case INSTANCES_CURSOR:
        Calendar calendar = Calendar.getInstance();
        data.moveToFirst();
        while (!data.isAfterLast()) {
          calendar.setTimeInMillis(data.getLong(1));
          DateTime dateTime = CalendarHelper.convertDateToDateTime(calendar.getTime());
          dateTime2InstanceMap.put(dateTime, data.getLong(0));
          selectedDates.add(dateTime);
          data.moveToNext();
        }
        refreshView();
        break;
      case INSTANCE_STATUS_CURSOR:
        data.moveToFirst();
        while (!data.isAfterLast()) {
          instance2TransactionMap.put(
              data.getLong(data.getColumnIndex(KEY_INSTANCEID)),
              data.getLong(data.getColumnIndex(KEY_TRANSACTIONID)));
          data.moveToNext();
        }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {

  }

  private class CaldroidCustomAdapter extends CaldroidGridAdapter {

    public CaldroidCustomAdapter(Context context, int month, int year,
                                 Map<String, Object> caldroidData,
                                 Map<String, Object> extraData) {
      super(context, month, year, caldroidData,
          extraData);
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

      DateTime dateTime = this.datetimeList.get(position);

      if (dateTime2InstanceMap.get(dateTime) != null) {
        state.setVisibility(View.VISIBLE);
        Long transactionId = instance2TransactionMap.get(dateTime2InstanceMap.get(dateTime));
        if (transactionId == null) {
          state.setImageResource(R.drawable.ic_stat_open_24dp);
          framelayout.setContentDescription(getString(R.string.plan_instance_state_open));
        } else if (transactionId == 0L) {
          state.setImageResource(R.drawable.ic_stat_cancelled_24dp);
          framelayout.setContentDescription(getString(R.string.plan_instance_state_cancelled));
        } else {
          state.setImageResource(R.drawable.ic_stat_applied_24dp);
          framelayout.setContentDescription(getString(R.string.plan_instance_state_applied));
        }
        boolean brightColor = Utils.isBrightColor(getArguments().getInt(DatabaseConstants.KEY_COLOR));
        cell.setTextColor(getContext().getResources().getColor(
            brightColor ? R.color.cell_text_color : R.color.cell_text_color_dark));
        DrawableCompat.setTint(state.getDrawable(), brightColor ? Color.BLACK : Color.WHITE);
      } else {
        state.setVisibility(View.GONE);
      }

      return framelayout;
    }

    @Override
    protected void resetCustomResources(CellView cellView) {
      int accountColor = getArguments().getInt(DatabaseConstants.KEY_COLOR);
      StateListDrawable stateListDrawable = new StateListDrawable();
      int todayDrawable = darkThemeSelected ? R.drawable.red_border_dark : R.drawable.red_border;
      GradientDrawable todaySelected =
          (GradientDrawable) getResources().getDrawable(todayDrawable).mutate();
      todaySelected.setColor(accountColor);
      stateListDrawable.addState(
          new int[]{R.attr.state_date_selected, R.attr.state_date_today},
          todaySelected);
      stateListDrawable.addState(
          new int[]{R.attr.state_date_selected},
          new ColorDrawable(accountColor));
      stateListDrawable.addState(
          new int[]{R.attr.state_date_today},
          getResources().getDrawable(todayDrawable));
      stateListDrawable.addState(
          new int[]{R.attr.state_date_prev_next_month},
          new ColorDrawable(getContext().getResources().getColor(
              darkThemeSelected ? R.color.caldroid_333 : R.color.caldroid_white)));
      stateListDrawable.addState(
          new int[]{},
          new ColorDrawable(getContext().getResources().getColor(
              darkThemeSelected ? R.color.caldroid_black : R.color.caldroid_white)));
      cellView.setBackgroundDrawable(stateListDrawable);

      cellView.setTextColor(defaultTextColorRes);
    }
  }
}
