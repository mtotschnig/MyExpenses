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
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.UiUtils;

import butterknife.BindView;
import butterknife.ButterKnife;


public class OnboardingActivity extends SyncBackendSetupActivity implements ViewPager.OnPageChangeListener {

  @BindView(R.id.viewpager)
  ViewPager pager;
  private MyPagerAdapter pagerAdapter;
  @BindView(R.id.navigation_next)
  View navigationNext;
  @BindView(R.id.navigation_finish)
  View navigationFinish;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
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
      int current_version = DistribHelper.getVersionNumber();
      PrefKey.CURRENT_VERSION.putInt(current_version);
      PrefKey.FIRST_INSTALL_VERSION.putInt(current_version);
      Intent intent = new Intent(this, MyExpenses.class);
      startActivity(intent);
      finish();
    } else {
      String message = "Unknown error while setting up account";
      AcraHelper.report(message);
      Snackbar snackbar = Snackbar.make(pager, message, Snackbar.LENGTH_LONG);
      UiUtils.configureSnackbarForDarkTheme(snackbar);
      snackbar.show();
    }
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
