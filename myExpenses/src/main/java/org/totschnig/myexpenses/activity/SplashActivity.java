package org.totschnig.myexpenses.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.RestoreFromCloudDialogFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.task.RestoreTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Result;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_INIT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS;


public class SplashActivity extends SyncBackendSetupActivity {

  private static final int REQUEST_CODE_RESOLUTION = 1;
  @BindView(R.id.viewpager)
  ViewPager pager;
  private MyPagerAdapter pagerAdapter;
  @State String accountName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdOnboarding());
    if (PrefKey.CURRENT_VERSION.getInt(-1) != -1) {
      super.onCreate(null);
      startTaskExecution(TaskExecutionFragment.TASK_INIT, null, null, 0);
      return;
    }
    super.onCreate(savedInstanceState);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setContentView(R.layout.onboarding);
    ButterKnife.bind(this);
    //setupToolbar(false);
    pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(pagerAdapter);
    if (MyApplication.isInstrumentationTest()) {
      PreferenceManager.setDefaultValues(this, MyApplication.getTestId(), Context.MODE_PRIVATE,
          R.xml.preferences, true);
    } else {
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      pager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    final Window window = getWindow();
    int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
      window.setStatusBarColor(Color.TRANSPARENT);
    }
    window.addFlags(flags);
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
    pager.setCurrentItem(1, true);
  }

  @Override
  public void onBackPressed() {
    if (pager != null && pager.getCurrentItem() == 1) {
      pager.setCurrentItem(0);
    } else {
      super.onBackPressed();
    }
  }

  public void showMoreOptions(View view) {
    getDataFragment().showMoreOptions(view);
  }

  private OnboardingDataFragment getDataFragment() {
    return (OnboardingDataFragment) getSupportFragmentManager().findFragmentByTag(
        pagerAdapter.getFragmentName(1));
  }

  public void finishOnboarding() {
    startDbWriteTask(false);
  }

  @Override
  public Model getObject() {
    return getDataFragment().buildAccount();
  }

  @Override
  public void onPostExecute(Object result) {
    super.onPostExecute(result);
    if (result != null) {
      getStarted();
    } else {
      String message = "Unknown error while setting up account";
      AcraHelper.report(message);
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

  private void showSnackbar(String message) {
    showSnackbar(message, Snackbar.LENGTH_LONG);
  }

  @Override
  protected boolean createAccountTaskShouldReturnDataList() {
    return true;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result result = (Result) o;
    switch (taskId) {
      case TASK_CREATE_SYNC_ACCOUNT:
      case TASK_FETCH_SYNC_ACCOUNT_DATA: {
        if (result.success) {
          supportInvalidateOptionsMenu();
          if (result.extra != null && result.extra.length > 2) {
            accountName = (String) result.extra[0];
            List<String> backupList = (List<String>) result.extra[1];
            List<AccountMetaData> syncAccountList = (List<AccountMetaData>) result.extra[2];
            if (backupList.size() > 0 || syncAccountList.size() > 0) {
              RestoreFromCloudDialogFragment.newInstance(backupList, syncAccountList)
                  .show(getSupportFragmentManager(), "RESTORE_FROM_CLOUD");
              break;
            }
          }
          showSnackbar("Neither backups nor sync accounts found");
        } else {
          if (result.extra != null && result.extra.length > 0 && result.extra[0] instanceof PendingIntent) {
            try {
              startIntentSenderForResult(((PendingIntent) result.extra[0]).getIntentSender(), REQUEST_CODE_RESOLUTION, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
              Timber.e(e, "Exception while starting resolution activity");
            }
          } else {
            showSnackbar("Unable to set up account");
          }
        }
        break;
      }
      case TASK_SETUP_FROM_SYNC_ACCOUNTS: {
        if (result.success) {
          getStarted();
        }
        break;
      }
      case TASK_INIT : {
        if (!isFinishing()) {
          Intent intent = new Intent(this, MyExpenses.class);
          startActivity(intent);
          finish();
        }
        break;
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_RESOLUTION:
        showSnackbar("Please try again");
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
        break;
    }
  }

  @Override
  protected void onPostRestoreTask(Result result) {
    super.onPostRestoreTask(result);
    if (result.success) {
      restartAfterRestore();
    }
  }

  public void setupFromBackup(String backup, int restorePlanStrategie) {
    Bundle arguments = new Bundle(3);
    arguments.putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, accountName);
    arguments.putString(RestoreTask.KEY_BACKUP_FROM_SYNC, backup);
    arguments.putInt(RestoreTask.KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategie);
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
      switch(pos) {
        case 0: return OnboardingUiFragment.newInstance();
        case 1:
        default: return OnboardingDataFragment.newInstance();
      }
    }

    @Override
    public int getCount() {
      return 2;
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
