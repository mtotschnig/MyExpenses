package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.FontSizeDialogFragment;
import org.totschnig.myexpenses.fragment.OnboardingDataFragment;
import org.totschnig.myexpenses.fragment.OnboardingUiFragment;

public class OnboardingActivity extends ProtectedFragmentActivity {

  private ViewPager pager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.onboarding);
    setupToolbar(false);
    pager = (ViewPager) findViewById(R.id.viewpager);
    pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
    if (MyApplication.isInstrumentationTest()) {
      PreferenceManager.setDefaultValues(this, MyApplication.getTestId(), Context.MODE_PRIVATE,
          R.xml.preferences, true);
    } else {
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return true;
  }

  public void openFontSizeDialog(View view) {
    FontSizeDialogFragment.newInstance().show(getSupportFragmentManager(), "FONT_SIZE");
  }

  public void navigate_next(View view) {
    pager.setCurrentItem(1, true);
  }

  private class MyPagerAdapter extends FragmentPagerAdapter {

    public MyPagerAdapter(FragmentManager fm) {
      super(fm);
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
