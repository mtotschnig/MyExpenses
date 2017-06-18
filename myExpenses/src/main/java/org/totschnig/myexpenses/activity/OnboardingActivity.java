package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.RestoreFromCloudDialogFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

import static android.text.TextUtils.join;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_RESTORE_FROM_SYNC_ACCOUNTS;


public class OnboardingActivity extends SyncBackendSetupActivity implements ViewPager.OnPageChangeListener {

  @BindView(R.id.viewpager)
  ViewPager pager;
  private MyPagerAdapter pagerAdapter;
  @BindView(R.id.navigation_next)
  View navigationNext;
  @BindView(R.id.navigation_finish)
  View navigationFinish;
  @State String accountName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setContentView(R.layout.onboarding);
    ButterKnife.bind(this);
    setupToolbar(false);
    pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
    pager.setAdapter(pagerAdapter);
    if (MyApplication.isInstrumentationTest()) {
      PreferenceManager.setDefaultValues(this, MyApplication.getTestId(), Context.MODE_PRIVATE,
          R.xml.preferences, true);
    } else {
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
    pager.addOnPageChangeListener(this);
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

  public void navigate_next(View view) {
    pager.setCurrentItem(1, true);
  }

  @Override
  public void onBackPressed() {
    if (pager.getCurrentItem() == 1) {
      pager.setCurrentItem(0);
    } else {
      super.onBackPressed();
    }
  }

  public void showMoreOptions(View view) {
    ((OnboardingDataFragment) getSupportFragmentManager().findFragmentByTag(
        pagerAdapter.getFragmentName(1))).showMoreOptions(view);
  }

  public void finishOnboarding(View view) {
    startDbWriteTask(false);
  }

  @Override
  public void onPageScrolled(int i, float v, int i1) {

  }

  @Override
  public void onPageSelected(int i) {
    navigationNext.setVisibility(i==0 ? View.VISIBLE : View.GONE);
    navigationFinish.setVisibility(i==1 ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onPageScrollStateChanged(int i) {

  }

  @Override
  public Model getObject() {
    return ((OnboardingDataFragment) getSupportFragmentManager().findFragmentByTag(
        pagerAdapter.getFragmentName(1))).buildAccount();
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
    Snackbar snackbar = Snackbar.make(pager, message, Snackbar.LENGTH_LONG);
    UiUtils.configureSnackbarForDarkTheme(snackbar);
    snackbar.show();
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
        String message;
        if (result.success) {
          invalidateOptionsMenu();
          if (result.extra != null) {
            accountName = (String) result.extra[0];
            List<String> backupList = (List<String>) result.extra[1];
            List<String> syncAccountList = (List<String>) result.extra[2];
            if (backupList.size() > 0 || syncAccountList.size() > 0) {
              RestoreFromCloudDialogFragment.newInstance(backupList, syncAccountList)
                  .show(getSupportFragmentManager(), "RESTORE_FROM_CLOUD");
              break;
            } else {
              message = "Neither backups nor sync accounts found";
            }
          } else {
            message = "Unable to retrieve information from sync backend";
          }
        } else {
          message = "Unable to set up account";
        }
        showSnackbar(message);
        break;
      }
      case TASK_RESTORE_FROM_SYNC_ACCOUNTS: {
        if (result.success) {
          getStarted();
        }
        break;
      }
    }
  }

  public void restoreFromBackup(String backup) {
    showSnackbar(accountName + " " + backup);//TODO
  }

  public void restoreFromSyncAccounts(List<String> syncAccounts) {
    startTaskExecution(TaskExecutionFragment.TASK_RESTORE_FROM_SYNC_ACCOUNTS,
        syncAccounts.toArray(new String[syncAccounts.size()]), accountName, R.string.pref_restore_title);
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
}
