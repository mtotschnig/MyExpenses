package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CaldroidListener;
import com.roomorama.caldroid.CalendarHelper;
import com.roomorama.caldroid.CellView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo;
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import icepick.Icepick;
import icepick.State;

public class PlanMonthFragment extends CaldroidFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TOOLBAR_TITLE = "toolbarTitle";
  private static final String KEY_READ_ONLY = "readoOnly";
  private LoaderManager mManager;
  public static final int INSTANCES_CURSOR = 1;
  public static final int INSTANCE_STATUS_CURSOR = 2;
  private boolean readOnly;

  StateListDrawable stateListDrawable;

  @State
  HashMap<Long, Long> instance2TransactionMap = new HashMap<>();

  //caldroid fragment operates on Dates set to Midnight. We want to store the exact timestamp in order
  //create the transactions with the exact date provided by the calendar
  @State
  HashMap<DateTime, Long> dateTime2TimeStampMap = new HashMap<>();

  public static PlanMonthFragment newInstance(String title, long templateId, long planId, int color,
                                              boolean readOnly) {
    PlanMonthFragment f = new PlanMonthFragment();
    Bundle args = new Bundle();
    args.putString(TOOLBAR_TITLE, title);
    args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.CaldroidCustom);
    args.putLong(DatabaseConstants.KEY_PLANID, planId);
    args.putInt(DatabaseConstants.KEY_COLOR, color);
    args.putLong(DatabaseConstants.KEY_ROWID, templateId);
    args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false);
    args.putBoolean(KEY_READ_ONLY, readOnly);
    args.putInt(CaldroidFragment.START_DAY_OF_WEEK,
        Utils.getFirstDayOfWeekFromPreferenceWithFallbackToLocale(Locale.getDefault()));
    f.setArguments(args);
    return f;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    readOnly = requireArguments().getBoolean(KEY_READ_ONLY);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setCaldroidListener(new CaldroidListener() {
      @Override
      public void onChangeMonth(int month, int year) {
        requireLoader(INSTANCES_CURSOR);
      }
    });
    setupStateListDrawable();
  }

  private void setupStateListDrawable() {
    int accountColor = requireArguments().getInt(DatabaseConstants.KEY_COLOR);
    stateListDrawable = new StateListDrawable();
    final int surfaceColor = UiUtils.getColor(requireContext(), R.attr.colorSurface);
    int todayDrawableResId = R.drawable.red_border;
    GradientDrawable today = (GradientDrawable) AppCompatResources.getDrawable(requireContext(), todayDrawableResId).mutate();
    GradientDrawable todaySelected = (GradientDrawable) AppCompatResources.getDrawable(requireContext(), todayDrawableResId).mutate();
    todaySelected.setColor(accountColor);
    today.setColor(surfaceColor);
    stateListDrawable.addState(new int[]{android.R.attr.state_activated},
        new ColorDrawable(ResourcesCompat.getColor(getResources(), R.color.appDefault, null)));
    stateListDrawable.addState(
        new int[]{R.attr.state_date_selected, R.attr.state_date_today},
        todaySelected);
    stateListDrawable.addState(
        new int[]{R.attr.state_date_selected},
        new ColorDrawable(accountColor));
    stateListDrawable.addState(
        new int[]{R.attr.state_date_today},
        today);
    stateListDrawable.addState(
        new int[]{R.attr.state_date_prev_next_month},
        new ColorDrawable(ResourcesCompat.getColor(getResources(), R.color.caldroid_state_date_prev_next_month, null)));
    stateListDrawable.addState(
        new int[]{},
        new ColorDrawable(surfaceColor));
  }

  private void requireLoader(int loaderId) {
    Utils.requireLoader(mManager, loaderId, null, this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mManager = LoaderManager.getInstance(this);
    View view = super.onCreateView(inflater, container, savedInstanceState);
    Toolbar toolbar = view.findViewById(R.id.calendar_toolbar);
    toolbar.setOnMenuItemClickListener(item -> {
      ((ProtectedFragmentActivity) requireActivity()).dispatchCommand(item.getItemId(),
          ManageTemplates.HelpVariant.plans.name());
      return true;
    });
    toolbar.inflateMenu(R.menu.help_with_icon);
    toolbar.setTitle(requireArguments().getString(TOOLBAR_TITLE));

    requireLoader(INSTANCE_STATUS_CURSOR);
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }


  @Override
  public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
    return new CaldroidCustomAdapter(getActivity(), month, year, getCaldroidData(), extraData);
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case INSTANCES_CURSOR:
        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarProviderProxy.INSTANCES_URI.buildUpon();
        DateTime startOfMonth = new DateTime(year, month, 1, 0, 0, 0, 0);
        long start = startOfMonth.minusDays(7)
            .getMilliseconds(TimeZone.getDefault());
        long end = startOfMonth.getEndOfMonth().plusDays(7)
            .getMilliseconds(TimeZone.getDefault());
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return new CursorLoader(
            requireActivity(),
            builder.build(),
            null,
            String.format(Locale.US, CalendarContract.Instances.EVENT_ID + " = %d",
                requireArguments().getLong(DatabaseConstants.KEY_PLANID)),
            null,
            null);
      case INSTANCE_STATUS_CURSOR:
        return new CursorLoader(
            requireActivity(),
            TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            new String[]{
                KEY_TEMPLATEID,
                KEY_INSTANCEID,
                KEY_TRANSACTIONID
            },
            KEY_TEMPLATEID + " = ?",
            new String[]{String.valueOf(getTemplateId())},
            null);
    }
    throw new IllegalArgumentException();
  }

  private long getTemplateId() {
    return requireArguments().getLong(KEY_ROWID);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
    if (data == null) {
      return;
    }
    switch (loader.getId()) {
      case INSTANCES_CURSOR:
        Calendar calendar = Calendar.getInstance();
        data.moveToFirst();
        clearSelectedDates();
        while (!data.isAfterLast()) {
          long timeInMillis = data.getLong(
              data.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
          calendar.setTimeInMillis(timeInMillis);
          DateTime dateTime = CalendarHelper.convertDateToDateTime(calendar.getTime());
          selectedDates.add(dateTime);
          dateTime2TimeStampMap.put(dateTime, timeInMillis);
          data.moveToNext();
        }
        refreshView();
        break;
      case INSTANCE_STATUS_CURSOR:
        data.moveToFirst();
        instance2TransactionMap.clear();
        while (!data.isAfterLast()) {
          instance2TransactionMap.put(
              data.getLong(data.getColumnIndexOrThrow(KEY_INSTANCEID)),
              data.getLong(data.getColumnIndexOrThrow(KEY_TRANSACTIONID)));
          data.moveToNext();
        }
        refreshView();
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {

  }

  private PlanInstanceState getState(Long id) {
    Long transactionId = instance2TransactionMap.get(id);
    if (transactionId == null) {
      return PlanInstanceState.OPEN;
    } else if (transactionId != 0L) {
      return PlanInstanceState.APPLIED;
    } else {
      return PlanInstanceState.CANCELLED;
    }
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
      View frameLayout;

      // For reuse
      if (convertView == null) {
        //noinspection InflateParams
        frameLayout = localInflater.inflate(R.layout.plan_calendar_cell, null);
      } else {
        frameLayout = convertView;
      }

      CellView cell = (CellView) frameLayout.findViewById(R.id.cell);
      ImageView state = (ImageView) frameLayout.findViewById(R.id.state);

      customizeTextView(position, cell);

      DateTime dateTime = this.datetimeList.get(position);

      if (selectedDates.contains(dateTime)) {
        state.setVisibility(View.VISIBLE);
        PlanInstanceState planInstanceState = getState(CalendarProviderProxy.calculateId(dateTime));
        boolean brightColor = ColorUtils.isBrightColor(requireArguments().getInt(DatabaseConstants.KEY_COLOR));
        int themeResId = brightColor ? R.style.LightBackground : R.style.DarkBackground;
        switch (planInstanceState) {
          case OPEN:
            state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_open, themeResId));
            frameLayout.setContentDescription(getString(R.string.plan_instance_state_open));
            break;
          case APPLIED:
            state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_applied, themeResId));
            frameLayout.setContentDescription(getString(R.string.plan_instance_state_applied));
            break;
          case CANCELLED:
            state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_cancelled, themeResId));
            frameLayout.setContentDescription(getString(R.string.plan_instance_state_cancelled));
            break;
        }

        cell.setTextColor(ResourcesCompat.getColor(getResources(),
            brightColor ? R.color.cell_text_color : R.color.cell_text_color_dark, null));
        if (!readOnly) {
          final TemplatesList templatesList = (TemplatesList) requireParentFragment();
          final long instanceId = getPlanInstanceForPosition(position);
          templatesList.configureOnClickPopup(frameLayout,
              new PlanInstanceInfo(getTemplateId(), instanceId, getDateForPosition(position), instance2TransactionMap.get(instanceId), planInstanceState), null, null);
        }
      } else {
        state.setVisibility(View.GONE);
        frameLayout.setOnClickListener(null);
      }

      return frameLayout;
    }

    private long getPlanInstanceForPosition(int position) {
      final Long date = dateTime2TimeStampMap.get(datetimeList.get(position));
      return date != null ? CalendarProviderProxy.calculateId(date) : -1;
    }

    private long getDateForPosition(int position) {
      final Long date = dateTime2TimeStampMap.get(datetimeList.get(position));
      return date != null ? date : System.currentTimeMillis();
    }

    @Override
    protected void resetCustomResources(CellView cellView) {
      cellView.setBackground(stateListDrawable.mutate().getConstantState().newDrawable());

      cellView.setTextColor(defaultTextColorRes);
    }

    @Override
    public boolean isEnabled(int position) {
      return selectedDates.contains(this.datetimeList.get(position));
    }
  }
}
