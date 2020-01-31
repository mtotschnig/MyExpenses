package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.RestoreFromCloudDialogFragment;
import org.totschnig.myexpenses.fragment.OnBoardingPrivacyFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.task.RestoreTask;
import org.totschnig.myexpenses.task.SyncAccountTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_INIT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS;


public class SplashActivity extends SyncBackendSetupActivity {
  @BindView(R.id.viewpager)
  ViewPager pager;
  private MyPagerAdapter pagerAdapter;
  @State
  String accountName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeIdOnboarding());
    if (PrefKey.CURRENT_VERSION.getInt(-1) != -1) {
      super.onCreate(null);
      startTaskExecution(TaskExecutionFragment.TASK_INIT, null, null, 0);
      return;
    }
    if (MyApplication.isInstrumentationTest()) {
      PreferenceManager.setDefaultValues(this, MyApplication.getTestId(), Context.MODE_PRIVATE,
          R.xml.preferences, true);
    } else {
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
    super.onCreate(savedInstanceState);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setContentView(R.layout.onboarding);
    ButterKnife.bind(this);
    //setupToolbar(false);
    pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(pagerAdapter);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //skip Help
    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  public void navigate_next() {
    final int currentItem = pager.getCurrentItem();
    pager.setCurrentItem(currentItem + 1, true);
  }

  @Override
  public void onBackPressed() {
    if (pager != null) {
      final int currentItem = pager.getCurrentItem();
      if (currentItem > 0) {
        pager.setCurrentItem(currentItem - 1);
        return;
      }
    }
    super.onBackPressed();
  }

  public void showMoreOptions(View view) {
    getDataFragment().showMoreOptions(view);
  }

  private OnboardingDataFragment getDataFragment() {
    return (OnboardingDataFragment) getSupportFragmentManager().findFragmentByTag(
        pagerAdapter.getFragmentName(pagerAdapter.getCount()-1));
  }

  public void finishOnboarding() {
    startDbWriteTask();
  }

  @Override
  public Model getObject() {
    return getDataFragment().buildAccount();
  }

  @Override
  public void onPostExecute(Uri result) {
    super.onPostExecute(result);
    if (result != null) {
      getStarted();
    } else {
      String message = "Unknown error while setting up account";
      CrashHandler.report(message);
      showSnackbar(message);
    }
  }

  private void getStarted() {
    int current_version = DistribHelper.getVersionNumber();
    PrefKey.CURRENT_VERSION.putInt(current_version);
    PrefKey.FIRST_INSTALL_VERSION.putInt(current_version);
    Intent intent = new Intent(this, MyExpenses.class);
    startActivity(intent);
    finish();
  }

  @Override
  protected boolean createAccountTaskShouldReturnDataList() {
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.QUIT_COMMAND) {
      finish();
      return true;
    }
    return false;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TASK_CREATE_SYNC_ACCOUNT:
      case TASK_FETCH_SYNC_ACCOUNT_DATA: {
        Exceptional<SyncAccountTask.Result> resultExceptional = (Exceptional<SyncAccountTask.Result>) o;
        if (resultExceptional.isPresent()) {
          getDataFragment().setupMenu();
          SyncAccountTask.Result result = resultExceptional.get();
          if (result.backups != null && result.syncAccounts != null) {
            accountName = result.accountName;
            if (result.backups.size() > 0 || result.syncAccounts.size() > 0) {
              if (Stream.of(result.syncAccounts).map(accountMetaData -> accountMetaData.uuid()).distinct().count() < result.syncAccounts.size()) {
                showSnackbar("Found accounts with duplicate uuids");
              } else {
                RestoreFromCloudDialogFragment.newInstance(result.backups, result.syncAccounts)
                    .show(getSupportFragmentManager(), "RESTORE_FROM_CLOUD");
              }
              break;
            }
          }
          showSnackbar("Neither backups nor sync accounts found");
        } else {
          showSnackbar(resultExceptional.getException().getMessage());
        }
        break;
      }
      case TASK_SETUP_FROM_SYNC_ACCOUNTS: {
        Result result = (Result) o;
        if (result.isSuccess()) {
          getStarted();
        }
        break;
      }
      case TASK_INIT: {
        Result result = (Result) o;
        if (!isFinishing()) {
          if (result.isSuccess()) {
            Intent intent = new Intent(this, MyExpenses.class);
            startActivity(intent);
            finish();
          } else {
            MessageDialogFragment f = MessageDialogFragment.newInstance(
                0,
                result.print(this),
                new MessageDialogFragment.Button(android.R.string.ok, R.id.QUIT_COMMAND, null),
                null,
                null);
            f.setCancelable(false);
            f.show(getSupportFragmentManager(), "INIT_FAILURE");
          }
        }
        break;
      }
    }
  }

  @Override
  protected void onPostRestoreTask(Result result) {
    super.onPostRestoreTask(result);
    String msg = result.print(this);
    if (!TextUtils.isEmpty(msg)) {
      showSnackbar(msg);
    }
    if (result.isSuccess()) {
      restartAfterRestore();
    }
  }

  public void setupFromBackup(String backup, int restorePlanStrategy, String password) {
    Bundle arguments = new Bundle(4);
    arguments.putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName);
    arguments.putString(RestoreTask.KEY_BACKUP_FROM_SYNC, backup);
    arguments.putInt(RestoreTask.KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategy);
    arguments.putString(RestoreTask.KEY_PASSWORD, password);
    doRestore(arguments);
  }

  public void setupFromSyncAccounts(List<AccountMetaData> syncAccounts) {
    startTaskExecution(TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS,
        Stream.of(syncAccounts).map(AccountMetaData::uuid).toArray(size -> new String[size]),
        accountName, R.string.progress_dialog_fetching_data_from_sync_backend);
  }

  private class MyPagerAdapter extends FragmentPagerAdapter {

    MyPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    String getFragmentName(int currentPosition) {
      return FragmentPagerAdapter.makeFragmentName(R.id.viewpager, getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(int pos) {
      switch (pos) {
        case 0:
          return OnboardingUiFragment.newInstance();
        case 1:
          if (showPrivacyPage())
            return OnBoardingPrivacyFragment.newInstance();
        default:
          return OnboardingDataFragment.newInstance();
      }
    }

    @Override
    public int getCount() {
      return showPrivacyPage() ?  3 : 2;
    }

    private boolean showPrivacyPage() {
      return DistribHelper.getDistribution().supportsTrackingAndCrashReporting();
    }
  }

  public void editAccountColor(View view) {
    getDataFragment().editAccountColor();
  }

  @Override
  protected int getSnackbarContainerId() {
    return R.id.viewpager;
  }
}
