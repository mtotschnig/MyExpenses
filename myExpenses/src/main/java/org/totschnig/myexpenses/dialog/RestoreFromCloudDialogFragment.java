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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.OnboardingActivity;
import org.totschnig.myexpenses.databinding.RestoreFromCloudBinding;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class RestoreFromCloudDialogFragment extends BaseDialogFragment
    implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener, DialogUtils.CalendarRestoreStrategyChangedListener {
  private static final String KEY_BACKUP_LIST = "backupList";
  private static final String KEY_SYNC_ACCOUNT_LIST = "syncAccountList";
  private RadioGroup restorePlanStrategy;
  private RadioGroup.OnCheckedChangeListener calendarRestoreButtonCheckedChangeListener;
  private ArrayAdapter<String> backupAdapter;
  private RestoreFromCloudBinding binding;

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
    AlertDialog.Builder builder = initBuilderWithBinding(() -> {
      binding = RestoreFromCloudBinding.inflate(materialLayoutInflater);
      return binding;
    });
    binding.passwordLayout.passwordLayout.setHint(getString(R.string.input_label_passphrase));
    binding.passwordLayout.passwordEdit.addTextChangedListener(new TextWatcher() {
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
      backupAdapter = new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_single_choice, backups);
      binding.backupList.setAdapter(backupAdapter);
      binding.backupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
      binding.backupList.setOnItemClickListener(this);
      restorePlanStrategy = DialogUtils.configureCalendarRestoreStrategy(binding.backupListContainer);
      if (restorePlanStrategy != null) {
        calendarRestoreButtonCheckedChangeListener =
            DialogUtils.buildCalendarRestoreStrategyChangedListener(getActivity(), this);
        restorePlanStrategy.setOnCheckedChangeListener(calendarRestoreButtonCheckedChangeListener);
      }
      binding.tabs.addTab(binding.tabs.newTab().setText(R.string.onboarding_restore_from_cloud_backup).setTag(binding.backupListContainer));
    }
    if (syncAccounts != null && syncAccounts.size() > 0) {
      binding.syncAccountList.setAdapter(new ArrayAdapter<>(getActivity(),
          android.R.layout.simple_list_item_multiple_choice, syncAccounts));
      binding.syncAccountList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
      binding.syncAccountList.setOnItemClickListener(this);
      binding.tabs.addTab(binding.tabs.newTab().setText(R.string.onboarding_restore_from_cloud_sync_accounts).setTag(binding.syncAccountListContainer));
    }
    binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
    setTabVisibility(binding.tabs.getTabAt(0), View.VISIBLE);

    final AlertDialog dialog = builder.setTitle(R.string.onboarding_restore_from_cloud)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, null)
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
      if (restorePlanStrategy.getCheckedRadioButtonId() == -1) {
        return false;
      }
      if (binding.passwordLayout.passwordLayout.getVisibility() == View.VISIBLE && TextUtils.isEmpty(binding.passwordLayout.passwordEdit.getText().toString())) {
        return false;
      }
    }
    return activeList.getCheckedItemCount() > 0;
  }

  private LinearLayout getActiveContent() {
    return getContentForTab(binding.tabs.getTabAt(binding.tabs.getSelectedTabPosition()));
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ArrayList<String> backups = getBackups();
    ArrayList<AccountMetaData> syncAccounts = getSyncAccounts();
    if (which == AlertDialog.BUTTON_POSITIVE) {
      OnboardingActivity activity = (OnboardingActivity) getActivity();
      LinearLayout contentForTab = getActiveContent();
      int id = contentForTab.getId();
      if (id == R.id.backup_list) {
        final String password = binding.passwordLayout.passwordLayout.getVisibility() == View.VISIBLE ? binding.passwordLayout.passwordEdit.getText().toString() : null;
        activity.setupFromBackup(backups.get(findListView(contentForTab).getCheckedItemPosition()),
            restorePlanStrategy.getCheckedRadioButtonId(), password);
      } else if (id == R.id.sync_account_list) {
        activity.setupFromSyncAccounts(Stream.of(syncAccounts)
            .filterIndexed((index, value) -> findListView(contentForTab).isItemChecked(index))
            .collect(Collectors.toList()));
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
      binding.passwordLayout.passwordLayout.setVisibility(backupAdapter.getItem(position).endsWith("enc") ? View.VISIBLE : View.GONE);
    }
    configureSubmit();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onCheckedChanged() {
    configureSubmit();
  }

  @Override
  public void onCalendarPermissionDenied() {
    restorePlanStrategy.setOnCheckedChangeListener(null);
    restorePlanStrategy.clearCheck();
    restorePlanStrategy.setOnCheckedChangeListener(calendarRestoreButtonCheckedChangeListener);
    configureSubmit();
  }
}
