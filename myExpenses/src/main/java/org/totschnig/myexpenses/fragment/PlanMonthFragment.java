package org.totschnig.myexpenses.fragment;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.android.calendar.CalendarContractCompat;
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
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import hirondelle.date4j.DateTime;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.fragment.PlannerFragmentKt.configureMenuInternalPlanInstances;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

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
  protected HashMap<Long, Long> instance2TransactionMap = new HashMap<>();

  //caldroid fragment operates on Dates set to Midnight. We want to store the exact timestamp in order
  //create the transactions with the exact date provided by the caldendar
  @State
  protected HashMap<DateTime, Long> dateTime2TimeStampMap = new HashMap<>();

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
    readOnly = getArguments().getBoolean(KEY_READ_ONLY);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setCaldroidListener(new CaldroidListener() {
      @Override
      public void onSelectDate(Date date, View view) {
        //not our concern
      }

      @Override
      public void onChangeMonth(int month, int year) {
        if (!readOnly && isVisible()) {
          ((ContextualActionBarFragment) getParentFragment()).finishActionMode();
        }
        requireLoader(INSTANCES_CURSOR);
      }

      @Override
      public void onGridCreated(GridView gridView) {
        if (!readOnly)
          ((TemplatesList) getParentFragment()).registerForContextualActionBar(gridView);
      }
    });
    setupStateListDrawable();
  }

  private void setupStateListDrawable() {
    int accountColor = getArguments().getInt(DatabaseConstants.KEY_COLOR);
    stateListDrawable = new StateListDrawable();
    final int surfaceColor = UiUtils.getColor(requireContext(), R.attr.colorSurface);
    int todayDrawableResId = R.drawable.red_border;
    GradientDrawable today = (GradientDrawable) AppCompatResources.getDrawable(requireContext(), todayDrawableResId).mutate();
    GradientDrawable todaySelected = (GradientDrawable) AppCompatResources.getDrawable(requireContext(), todayDrawableResId).mutate();
    todaySelected.setColor(accountColor);
    today.setColor(surfaceColor);
    stateListDrawable.addState(new int[]{android.R.attr.state_activated},
        new ColorDrawable(getContext().getResources().getColor(R.color.appDefault)));
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
        new ColorDrawable(getContext().getResources().getColor(R.color.caldroid_state_date_prev_next_month)));
    stateListDrawable.addState(
        new int[]{},
        new ColorDrawable(surfaceColor));
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new Dialog(getActivity(), getTheme()) {
      @Override
      public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (featureId == Window.FEATURE_CONTEXT_MENU) {
          return getParentFragment().onContextItemSelected(item);
        } else {
          return super.onMenuItemSelected(featureId, item);
        }
      }
    };
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
      ((ProtectedFragmentActivity) getActivity()).dispatchCommand(item.getItemId(),
          ManageTemplates.HelpVariant.plans.name());
      return true;
    });
    toolbar.inflateMenu(R.menu.help_with_icon);
    toolbar.setTitle(getArguments().getString(TOOLBAR_TITLE));

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
            getActivity(),
            builder.build(),
            null,
            String.format(Locale.US, CalendarContractCompat.Instances.EVENT_ID + " = %d",
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
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
    if (data == null) {
      return;
    }
    switch (loader.getId()) {
      case INSTANCES_CURSOR:
        Calendar calendar = Calendar.getInstance();
        data.moveToFirst();
        while (!data.isAfterLast()) {
          long timeInMillis = data.getLong(
              data.getColumnIndex(CalendarContractCompat.Instances.BEGIN));
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
              data.getLong(data.getColumnIndex(KEY_INSTANCEID)),
              data.getLong(data.getColumnIndex(KEY_TRANSACTIONID)));
          data.moveToNext();
        }
        refreshView();
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {

  }

  public void dispatchCommandSingle(int command, int position) {
    long instanceId = getPlanInstanceForPosition(position);
    final FragmentActivity activity = getActivity();
    final Bundle arguments = getArguments();
    final TemplatesList templatesList = (TemplatesList) getParentFragment();
    if (activity == null || arguments == null || templatesList == null) return;
    if (instanceId != -1) {
      if (command == R.id.CREATE_PLAN_INSTANCE_EDIT_COMMAND) {
        templatesList.dispatchCreateInstanceEdit(arguments.getLong(KEY_ROWID), instanceId, getDateForPosition(position));
      } else if (command == R.id.EDIT_PLAN_INSTANCE_COMMAND) {
        templatesList.dispatchEditInstance(instance2TransactionMap.get(instanceId));
      }
    }
  }

  public void dispatchCommandMultiple(int command, SparseBooleanArray positions) {
    ArrayList<Long[]> extra2dAL = new ArrayList<>();
    ArrayList<Long> objectIdsAL = new ArrayList<>();
    final ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
    final Bundle arguments = getArguments();
    final TemplatesList templatesList = (TemplatesList) getParentFragment();
    if (activity == null || arguments == null || templatesList == null) return;
    if (command == R.id.CREATE_PLAN_INSTANCE_SAVE_COMMAND) {
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          int position = positions.keyAt(i);
          long instanceId = getPlanInstanceForPosition(position);
          //ignore instances that are not open
          if (instanceId == -1 || instance2TransactionMap.get(instanceId) != null)
            continue;
          //pass event instance id and date as extra
          extra2dAL.add(new Long[]{instanceId, getDateForPosition(position)});
          objectIdsAL.add(arguments.getLong(KEY_ROWID));
        }
      }
      templatesList.dispatchCreateInstanceSaveDo(objectIdsAL.toArray(new Long[0]),
          extra2dAL.toArray(new Long[extra2dAL.size()][2]));
    } else if (command == R.id.CANCEL_PLAN_INSTANCE_COMMAND) {
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          int position = positions.keyAt(i);
          long instanceId = getPlanInstanceForPosition(position);
          if (instanceId == -1)
            continue;
          objectIdsAL.add(instanceId);
          extra2dAL.add(new Long[]{arguments.getLong(KEY_ROWID),
              instance2TransactionMap.get(instanceId)});
        }
      }
      templatesList.dispatchTask(
          TaskExecutionFragment.TASK_CANCEL_PLAN_INSTANCE,
          objectIdsAL.toArray(new Long[0]),
          extra2dAL.toArray(new Long[extra2dAL.size()][2]));
    } else if (command == R.id.RESET_PLAN_INSTANCE_COMMAND) {
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          int position = positions.keyAt(i);
          long instanceId = getPlanInstanceForPosition(position);
          if (instanceId == -1)
            continue;
          objectIdsAL.add(instanceId);
          //pass transactionId in extra
          extra2dAL.add(new Long[]{arguments.getLong(KEY_ROWID),
              instance2TransactionMap.get(instanceId)});
        }
      }
      templatesList.dispatchTask(
          TaskExecutionFragment.TASK_RESET_PLAN_INSTANCE,
          objectIdsAL.toArray(new Long[0]),
          extra2dAL.toArray(new Long[extra2dAL.size()][2]));
    }
  }

  private long getPlanInstanceForPosition(int position) {
    final Long date = dateTime2TimeStampMap.get(dateInMonthsList.get(position));
    return date != null ? CalendarProviderProxy.calculateId(date) : -1;
  }

  private long getDateForPosition(int position) {
    final Long date = dateTime2TimeStampMap.get(dateInMonthsList.get(position));
    return date != null ? date : System.currentTimeMillis();
  }

  public void configureMenu11(Menu menu, int count, AbsListView lv) {
    boolean withOpen = false, withApplied = false, withCancelled = false;
    SparseBooleanArray checkedItemPositions = lv.getCheckedItemPositions();
    for (int i = 0; i < checkedItemPositions.size(); i++) {
      if (checkedItemPositions.valueAt(i)) {
        long instanceId = getPlanInstanceForPosition(checkedItemPositions.keyAt(i));
        if (instanceId == -1)
          continue;
        switch (getState(instanceId)) {
          case APPLIED:
            withApplied = true;
            break;
          case CANCELLED:
            withCancelled = true;
            break;
          case OPEN:
            withOpen = true;
            break;
        }
        configureMenuInternalPlanInstances(menu, count, withOpen, withApplied, withCancelled);
      }
    }
  }

  public void configureMenuLegacy(Menu menu, ContextMenu.ContextMenuInfo menuInfo) {
    long instanceId = getPlanInstanceForPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
    if (instanceId != -1) {
      configureMenuInternalPlanInstances(menu, getState(instanceId));
    }
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
        //TODO investigate why passing parent to inflate leads to corrupted display
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
        Long transactionId = instance2TransactionMap.get(CalendarProviderProxy.calculateId(dateTime));
        boolean brightColor = ColorUtils.isBrightColor(getArguments().getInt(DatabaseConstants.KEY_COLOR));
        int themeResId = brightColor ? R.style.LightBackground : R.style.DarkBackground;
        if (transactionId == null) {
          state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_open, themeResId));
          frameLayout.setContentDescription(getString(R.string.plan_instance_state_open));
        } else if (transactionId == 0L) {
          state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_cancelled, themeResId));
          frameLayout.setContentDescription(getString(R.string.plan_instance_state_cancelled));
        } else {
          state.setImageBitmap(UiUtils.getTintedBitmapForTheme(getContext(), R.drawable.ic_stat_applied, themeResId));
          frameLayout.setContentDescription(getString(R.string.plan_instance_state_applied));
        }

        cell.setTextColor(getContext().getResources().getColor(
            brightColor ? R.color.cell_text_color : R.color.cell_text_color_dark));
      } else {
        state.setVisibility(View.GONE);
      }

      return frameLayout;
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
