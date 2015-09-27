package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.fragment.CsvImportParseFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class CsvImportActivity extends ProtectedFragmentActivity implements
    ConfirmationDialogFragment.ConfirmationDialogListener {

  public static final String KEY_DATA_READY = "KEY_DATA_READY";
  public static final String KEY_USAGE_RECORDED = "KEY_USAGE_RECORDED";
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  SectionsPagerAdapter mSectionsPagerAdapter;

  private boolean mDataReady = false;
  private boolean mUsageRecorded = false;
  private TabLayout mTabLayout;
  private ViewPager mViewPager;

  private void setmDataReady(boolean mDataReady) {
    this.mDataReady = mDataReady;
    mSectionsPagerAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_with_tabs);
    setupToolbar(true);
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(getString(R.string.pref_import_title, "CSV"));

    mViewPager = (ViewPager) findViewById(R.id.viewpager);

    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mTabLayout = (TabLayout) findViewById(R.id.tabs);

    //we only add the first tab, the second one once data has been parsed
    addTab(0);
    if (savedInstanceState != null) {
      mUsageRecorded = savedInstanceState.getBoolean(KEY_USAGE_RECORDED);
      if (savedInstanceState.getBoolean(KEY_DATA_READY)) {
        addTab(1);
        setmDataReady(true);
      }
    }
    mViewPager.setAdapter(mSectionsPagerAdapter);

    mTabLayout.setupWithViewPager(mViewPager);

    //hide FAB
    findViewById(R.id.CREATE_COMMAND).setVisibility(View.GONE);
  }

  private void addTab(int index) {
   switch(index) {
     case 0:
       mSectionsPagerAdapter.addFragment(CsvImportParseFragment.newInstance(), getString(R.string.menu_parse));
       break;
     case 1:
       mSectionsPagerAdapter.addFragment(CsvImportDataFragment.newInstance(), getString(R.string.csv_import_preview));
       break;
   }
  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.SET_HEADER_COMMAND:
      CsvImportDataFragment df = (CsvImportDataFragment) getSupportFragmentManager().findFragmentByTag(
          mSectionsPagerAdapter.getFragmentName(1));
      df.setHeader();
    }
  }

  @Override
  public void onNegative(Bundle args) {

  }

  @Override
  public void onDismissOrCancel(Bundle args) {

  }

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

/*    @Override
    public Fragment getItem(int position) {
     switch (position) {
       case 0:
         return CsvImportParseFragment.newInstance();
       case 1:
         return CsvImportDataFragment.newInstance();
     }
      return null;
    }*/

    public String getFragmentName(int currentPosition) {
      //http://stackoverflow.com/questions/7379165/update-data-in-listfragment-as-part-of-viewpager
      //would call this function if it were visible
      //return makeFragmentName(R.id.viewpager,currentPosition);
      return "android:switcher:"+R.id.viewpager+":"+getItemId(currentPosition);
    }
  }

  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    switch (taskId) {
      case TaskExecutionFragment.TASK_CSV_PARSE:
        if (result != null) {
          if (!mDataReady) {
            addTab(1);
            setmDataReady(true);
            // TabLayout does not seem to be designed for handling changes in the adapter autmoatically
            // https://code.google.com/p/android/issues/detail?id=182033
            mTabLayout.addTab(mTabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(1)));
          }
          CsvImportDataFragment df = (CsvImportDataFragment) getSupportFragmentManager().findFragmentByTag(
              mSectionsPagerAdapter.getFragmentName(1));
          if (df != null) df.setData((ArrayList<CSVRecord>) result);
          mViewPager.setCurrentItem(1);
        } else {
          Toast.makeText(this, R.string.parse_error_no_data_found, Toast.LENGTH_LONG).show();
        }
        break;
      case TaskExecutionFragment.TASK_CSV_IMPORT:
        Result r = (Result) result;
        if (r.success) {
          if (!mUsageRecorded) {
            recordUsage(ContribFeature.CSV_IMPORT);
            mUsageRecorded = true;
          }
          Integer imported = (Integer) r.extra[0];
          Integer failed = (Integer) r.extra[1];
          Integer discarded = (Integer) r.extra[2];
          String label = (String) r.extra[3];
          String msg = getString(R.string.import_transactions_success, imported, label) + ".";
          if (failed>0) {
            msg += " " + getString(R.string.csv_import_records_failed,failed);
          }
          if (discarded>0) {
            msg += " " + getString(R.string.csv_import_records_discarded,discarded);
          }
          Toast.makeText(this, msg,Toast.LENGTH_LONG).show();
        }
    }
  }

  @Override
  public void onProgressUpdate(Object progress) {
    if (progress instanceof String) {
      Toast.makeText(this, (String) progress, Toast.LENGTH_LONG).show();
    } else {
      super.onProgressUpdate(progress);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_DATA_READY, mDataReady);
    outState.putBoolean(KEY_USAGE_RECORDED,mUsageRecorded);
  }

  public long getAccountId() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getAccountId();
  }

  public String getCurrency() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getCurrency();
  }

  public QifDateFormat getDateFormat() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getDateFormat();
  }

  public Account.Type getAccountType() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getAccountType();
  }

}
