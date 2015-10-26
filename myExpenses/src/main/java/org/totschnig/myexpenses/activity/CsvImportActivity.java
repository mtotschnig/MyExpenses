package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.csv.CSVRecord;
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


public class CsvImportActivity extends TabbedActivity implements
    ConfirmationDialogFragment.ConfirmationDialogListener {

  public static final String KEY_DATA_READY = "KEY_DATA_READY";
  public static final String KEY_USAGE_RECORDED = "KEY_USAGE_RECORDED";

  private boolean mDataReady = false;
  private boolean mUsageRecorded = false;

  private void setmDataReady(boolean mDataReady) {
    this.mDataReady = mDataReady;
    mSectionsPagerAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(getString(R.string.pref_import_title, "CSV"));

    //hide FAB
    findViewById(R.id.CREATE_COMMAND).setVisibility(View.GONE);
  }

  @Override
  protected void setupTabs(Bundle savedInstanceState) {
    //we only add the first tab, the second one once data has been parsed
    addTab(0);
    if (savedInstanceState != null) {
      mUsageRecorded = savedInstanceState.getBoolean(KEY_USAGE_RECORDED);
      if (savedInstanceState.getBoolean(KEY_DATA_READY)) {
        addTab(1);
        setmDataReady(true);
      }
    }
  }

  private void addTab(int index) {
   switch(index) {
     case 0:
       mSectionsPagerAdapter.addFragment(CsvImportParseFragment.newInstance(), getString(
           R.string.menu_parse));
       break;
     case 1:
       mSectionsPagerAdapter.addFragment(CsvImportDataFragment.newInstance(), getString(
           R.string.csv_import_preview));
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
    CsvImportParseFragment pf = getParseFragment();
    return pf.getAccountId();
  }

  private CsvImportParseFragment getParseFragment() {
    return (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
  }

  public String getCurrency() {
    CsvImportParseFragment pf = getParseFragment();
    return pf.getCurrency();
  }

  public QifDateFormat getDateFormat() {
    CsvImportParseFragment pf = getParseFragment();
    return pf.getDateFormat();
  }

  public Account.Type getAccountType() {
    CsvImportParseFragment pf = getParseFragment();
    return pf.getAccountType();
  }

}
