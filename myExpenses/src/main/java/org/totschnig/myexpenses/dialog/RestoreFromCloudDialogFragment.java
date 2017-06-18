package org.totschnig.myexpenses.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ListView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.OnboardingActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class RestoreFromCloudDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener {
  private static final String KEY_BACKUP_LIST = "backupList";
  private static final String KEY_SYNC_ACCOUNT_LIST = "syncAccountList";
  @BindView(R.id.tabs)
  protected TabLayout tabLayout;
  @BindView(R.id.backup_list)
  protected ListView backupList;
  @BindView(R.id.sync_account_list)
  protected ListView syncAccountList;

  public static RestoreFromCloudDialogFragment newInstance(List<String> backupList, List<String> syncAccountList) {
    Bundle arguments = new Bundle(2);
    arguments.putStringArrayList(KEY_BACKUP_LIST, new ArrayList<>(backupList));
    arguments.putStringArrayList(KEY_SYNC_ACCOUNT_LIST, new ArrayList<>(syncAccountList));
    RestoreFromCloudDialogFragment fragment = new RestoreFromCloudDialogFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
      backupList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_single_choice, backups));
      backupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
      backupList.setOnItemClickListener(this);
      tabLayout.addTab(tabLayout.newTab().setText("From backup").setTag(backupList));
    }
    if (syncAccounts != null && syncAccounts.size() > 0) {
      syncAccountList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_multiple_choice, syncAccounts));
      syncAccountList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
      syncAccountList.setOnItemClickListener(this);
      tabLayout.addTab(tabLayout.newTab().setText("From Sync accounts").setTag(syncAccountList));
    }
    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(Tab tab) {
        setTabVisibility(tab, View.VISIBLE);
        configureSubmit(getContentForTab(tab));
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
    ListView listView = getContentForTab(tab);
    listView.setVisibility(visibility);
  }

  private ListView getContentForTab(Tab tab) {
    return (ListView) tab.getTag();
  }

  private void configureSubmit(ListView activeList) {
    Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
    if (button == null) {
      return;
    }
    boolean enabled = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      enabled = activeList.getCheckedItemCount() > 0;
    } else {
      SparseBooleanArray checkedItemPositions = activeList.getCheckedItemPositions();
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i)) {
          enabled = true;
          break;
        }
      }
    }
    button.setEnabled(enabled);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ArrayList<String> backups = getBackups();
    ArrayList<String> syncAccounts = getSyncAccounts();
    if (which == AlertDialog.BUTTON_POSITIVE) {
      OnboardingActivity activity = (OnboardingActivity) getActivity();
      ListView contentForTab = getContentForTab(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
      switch (contentForTab.getId()) {
        case R.id.backup_list:
          activity.setupFromBackup(backups.get(backupList.getCheckedItemPosition()));
          break;
        case R.id.sync_account_list:
          activity.setupFromSyncAccounts(Stream.of(syncAccounts)
              .filterIndexed((index, value) -> syncAccountList.isItemChecked(index))
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
    configureSubmit(((ListView) parent));
  }
}
