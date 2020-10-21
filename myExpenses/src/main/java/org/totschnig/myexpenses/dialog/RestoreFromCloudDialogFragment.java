package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.Tab;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.OnboardingActivity;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import butterknife.BindView;
import butterknife.ButterKnife;

public class RestoreFromCloudDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener, DialogUtils.CalendarRestoreStrategyChangedListener {
  private static final String KEY_BACKUP_LIST = "backupList";
  private static final String KEY_SYNC_ACCOUNT_LIST = "syncAccountList";
  @BindView(R.id.tabs)
  TabLayout tabLayout;
  @BindView(R.id.backup_list)
  LinearLayout backupListContainer;
  @BindView(R.id.sync_account_list)
  LinearLayout syncAccountListContainer;
  @BindView(R.id.passwordLayout)
  TextInputLayout passwordLayout;
  @BindView(R.id.passwordEdit)
  TextInputEditText passwordEdit;
  private RadioGroup restorePlanStrategie;
  private RadioGroup.OnCheckedChangeListener calendarRestoreButtonCheckedChangeListener;
  private ArrayAdapter<String> backupAdapter;

  public static RestoreFromCloudDialogFragment newInstance(List<String> backupList, List<AccountMetaData> syncAccountList) {
    Bundle arguments = new Bundle(2);
    arguments.putStringArrayList(KEY_BACKUP_LIST, new ArrayList<>(backupList));
    arguments.putParcelableArrayList(KEY_SYNC_ACCOUNT_LIST, new ArrayList<>(syncAccountList));
    RestoreFromCloudDialogFragment fragment = new RestoreFromCloudDialogFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(R.layout.restore_from_cloud);
    ButterKnife.bind(this, dialogView);
    passwordLayout.setHint(getString(R.string.input_label_passphrase));
    passwordEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        configureSubmit();
      }
    });
    ArrayList<String> backups = getBackups();
    ArrayList<AccountMetaData> syncAccounts = getSyncAccounts();
    if (backups != null && backups.size() > 0) {
      ListView backupList = findListView(backupListContainer);
      backupAdapter = new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_single_choice, backups);
      backupList.setAdapter(backupAdapter);
      backupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
      backupList.setOnItemClickListener(this);
      restorePlanStrategie = DialogUtils.configureCalendarRestoreStrategy(backupListContainer);
      if (restorePlanStrategie != null) {
        calendarRestoreButtonCheckedChangeListener =
            DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
        restorePlanStrategie.setOnCheckedChangeListener(calendarRestoreButtonCheckedChangeListener);
      }
      DialogUtils.configureCalendarRestoreStrategy(backupListContainer);
      tabLayout.addTab(tabLayout.newTab().setText(R.string.onboarding_restore_from_cloud_backup).setTag(backupListContainer));
    }
    if (syncAccounts != null && syncAccounts.size() > 0) {
      ListView syncAccountList = findListView(syncAccountListContainer);
      syncAccountList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_multiple_choice, syncAccounts));
      syncAccountList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
      syncAccountList.setOnItemClickListener(this);
      tabLayout.addTab(tabLayout.newTab().setText(R.string.onboarding_restore_from_cloud_sync_accounts).setTag(syncAccountListContainer));
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

    final AlertDialog dialog = builder.setTitle(R.string.onboarding_restore_from_cloud)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel,null)
        .create();
    dialog.setOnShowListener(new ButtonOnShowDisabler());
    return dialog;
  }

  private void setTabVisibility(Tab tab, int visibility) {
    LinearLayout list = getContentForTab(tab);
    list.setVisibility(visibility);
  }

  private LinearLayout getContentForTab(Tab tab) {
    return (LinearLayout) tab.getTag();
  }

  private ListView findListView(LinearLayout parent) {
    for (int i = 0; i < parent.getChildCount(); i++) {
      View child = parent.getChildAt(i);
      if (child instanceof ListView) {
        return ((ListView) child);
      }
    }
    return null;
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
    if (activeContent.getId() == R.id.backup_list) {
      if (restorePlanStrategie.getCheckedRadioButtonId() == -1) {
        return false;
      }
      if (passwordLayout.getVisibility() == View.VISIBLE && TextUtils.isEmpty(passwordEdit.getText().toString())) {
        return false;
      }
    }
    return activeList.getCheckedItemCount() > 0;
  }

  private LinearLayout getActiveContent() {
    return getContentForTab(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ArrayList<String> backups = getBackups();
    ArrayList<AccountMetaData> syncAccounts = getSyncAccounts();
    if (which == AlertDialog.BUTTON_POSITIVE) {
      OnboardingActivity activity = (OnboardingActivity) getActivity();
      LinearLayout contentForTab = getActiveContent();
      switch (contentForTab.getId()) {
        case R.id.backup_list:
          final String password = passwordLayout.getVisibility() == View.VISIBLE ? passwordEdit.getText().toString() : null;
          activity.setupFromBackup(backups.get(findListView(contentForTab).getCheckedItemPosition()),
              restorePlanStrategie.getCheckedRadioButtonId(), password);
          break;
        case R.id.sync_account_list:
          activity.setupFromSyncAccounts(Stream.of(syncAccounts)
              .filterIndexed((index, value) -> findListView(contentForTab).isItemChecked(index))
              .collect(Collectors.toList()));
          break;
      }
    }
  }

  private ArrayList<AccountMetaData> getSyncAccounts() {
    return getArguments().getParcelableArrayList(KEY_SYNC_ACCOUNT_LIST);
  }

  private ArrayList<String> getBackups() {
    return getArguments().getStringArrayList(KEY_BACKUP_LIST);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (((LinearLayout) parent.getParent()).getId() == R.id.backup_list) {
      passwordLayout.setVisibility(backupAdapter.getItem(position).endsWith("enc") ? View.VISIBLE : View.GONE);
    }
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
