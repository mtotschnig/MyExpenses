package org.totschnig.myexpenses.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.Tab;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.OnboardingActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RestoreFromCloudDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener, DialogUtils.CalendarRestoreStrategyChangedListener {
  private static final String KEY_BACKUP_LIST = "backupList";
  private static final String KEY_SYNC_ACCOUNT_LIST = "syncAccountList";
  @BindView(R.id.tabs)
  protected TabLayout tabLayout;
  @BindView(R.id.backup_list)
  protected LinearLayout backupListContainer;
  @BindView(R.id.sync_account_list)
  protected LinearLayout syncAccountListContainer;
  private RadioGroup restorePlanStrategie;
  private RadioGroup.OnCheckedChangeListener calendarRestoreButtonCheckedChangeListener;

  public static RestoreFromCloudDialogFragment newInstance(List<String> backupList, List<String> syncAccountList) {
    Bundle arguments = new Bundle(2);
    arguments.putStringArrayList(KEY_BACKUP_LIST, new ArrayList<>(backupList));
    arguments.putStringArrayList(KEY_SYNC_ACCOUNT_LIST, new ArrayList<>(syncAccountList));
    RestoreFromCloudDialogFragment fragment = new RestoreFromCloudDialogFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx = getActivity();
    @SuppressLint("InflateParams")
    final View view = LayoutInflater.from(ctx).inflate(R.layout.restore_from_cloud, null);
    ButterKnife.bind(this, view);
    ArrayList<String> backups = getBackups();
    ArrayList<String> syncAccounts = getSyncAccounts();
    if (backups != null && backups.size() > 0) {
      ListView backupList = findListView(backupListContainer);
      backupList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_single_choice, backups));
      backupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
      backupList.setOnItemClickListener(this);
      restorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(backupListContainer);
      if (restorePlanStrategie != null) {
        calendarRestoreButtonCheckedChangeListener =
            DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
        restorePlanStrategie.setOnCheckedChangeListener(calendarRestoreButtonCheckedChangeListener);
      }
      DialogUtils.configureCalendarRestoreStrategy(backupListContainer);
      tabLayout.addTab(tabLayout.newTab().setText("From backup").setTag(backupListContainer));
    }
    if (syncAccounts != null && syncAccounts.size() > 0) {
      ListView syncAccountList = findListView(syncAccountListContainer);
      syncAccountList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_multiple_choice, syncAccounts));
      syncAccountList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
      syncAccountList.setOnItemClickListener(this);
      tabLayout.addTab(tabLayout.newTab().setText("From Sync accounts").setTag(syncAccountListContainer));
    }
    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(Tab tab) {
        setTabVisibility(tab, View.VISIBLE);
        configureSubmit();
      }

      @Override
      public void onTabUnselected(Tab tab) {
        setTabVisibility(tab, View.GONE);
      }

      @Override
      public void onTabReselected(Tab tab) {

      }
    });
    setTabVisibility(tabLayout.getTabAt(0), View.VISIBLE);

    final AlertDialog dialog = new AlertDialog.Builder(ctx)
        .setTitle(R.string.onboarding_restore_from_cloud)
        .setView(view)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel,null)
        .create();
    dialog.setOnShowListener(new ButtonOnShowDisabler());
    return dialog;
  }

  private ArrayList<String> getBackups() {
    return getArguments().getStringArrayList(KEY_BACKUP_LIST);
  }

  private void setTabVisibility(Tab tab, int visibility) {
    LinearLayout list = getContentForTab(tab);
    list.setVisibility(visibility);
  }

  private LinearLayout getContentForTab(Tab tab) {
    return (LinearLayout) tab.getTag();
  }

  private ListView findListView(LinearLayout parent) {
    return (ListView) parent.findViewById(R.id.list);
  }

  private void configureSubmit() {
    Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
    if (button != null) {
      button.setEnabled(isReady());
    }
  }

  private boolean isReady() {
    LinearLayout activeContent = getActiveContent();
    ListView activeList = findListView(activeContent);
    if (activeContent.getId() == R.id.backup_list &&
        restorePlanStrategie.getCheckedRadioButtonId() == -1) {
      return false;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return activeList.getCheckedItemCount() > 0;
    } else {
      SparseBooleanArray checkedItemPositions = activeList.getCheckedItemPositions();
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i)) {
          return true;
        }
      }
    }
    return false;
  }

  private LinearLayout getActiveContent() {
    return getContentForTab(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ArrayList<String> backups = getBackups();
    ArrayList<String> syncAccounts = getSyncAccounts();
    if (which == AlertDialog.BUTTON_POSITIVE) {
      OnboardingActivity activity = (OnboardingActivity) getActivity();
      LinearLayout contentForTab = getActiveContent();
      switch (contentForTab.getId()) {
        case R.id.backup_list:
          activity.setupFromBackup(backups.get(findListView(contentForTab).getCheckedItemPosition()),
              restorePlanStrategie.getCheckedRadioButtonId());
          break;
        case R.id.sync_account_list:
          activity.setupFromSyncAccounts(Stream.of(syncAccounts)
              .filterIndexed((index, value) -> findListView(contentForTab).isItemChecked(index))
              .collect(Collectors.toList()));
          break;
      }
    }
  }

  private ArrayList<String> getSyncAccounts() {
    return getArguments().getStringArrayList(KEY_SYNC_ACCOUNT_LIST);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    configureSubmit();
  }

  @Override
  public void onCheckedChanged() {
    configureSubmit();
  }

  @Override
  public void onCalendarPermissionDenied() {
    restorePlanStrategie.setOnCheckedChangeListener(null);
    restorePlanStrategie.clearCheck();
    restorePlanStrategie.setOnCheckedChangeListener(calendarRestoreButtonCheckedChangeListener);
    configureSubmit();
  }
}
