package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public abstract class TabbedActivity extends ProtectedFragmentActivity {
  protected TabLayout mTabLayout;
  protected ViewPager mViewPager;
  /**
   * The {@link PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link FragmentStatePagerAdapter}.
   */
  SectionsPagerAdapter mSectionsPagerAdapter;

  protected int getLayoutRessourceId() {
    return R.layout.activity_with_tabs;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(getLayoutRessourceId());
    setupToolbar(true);

    mViewPager = findViewById(R.id.viewpager);

    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mTabLayout = findViewById(R.id.tabs);

    setupTabs();

    mViewPager.setAdapter(mSectionsPagerAdapter);

    mTabLayout.setupWithViewPager(mViewPager);
  }

  protected abstract void setupTabs();

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragments = new ArrayList<>();
    private final List<String> mFragmentTitles = new ArrayList<>();

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }


    public void addFragment(Fragment fragment, String title) {
      mFragments.add(fragment);
      mFragmentTitles.add(title);
    }

    @Override
    public Fragment getItem(int position) {
      return mFragments.get(position);
    }

    @Override
    public int getCount() {
      return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return mFragmentTitles.get(position);
    }

    public String getFragmentName(int currentPosition) {
      //http://stackoverflow.com/questions/7379165/update-data-in-listfragment-as-part-of-viewpager
      //would call this function if it were visible
      //return makeFragmentName(R.id.viewpager_main,currentPosition);
      return "android:switcher:"+ R.id.viewpager+":"+getItemId(currentPosition);
    }
  }

  @Override
  protected int getSnackbarContainerId() {
    return R.id.viewpager;
  }
}
