package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.test.util.Fixture;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;


/**
 * The whole package should be tested starting with a cleared database
 * adb shell pm clear org.totschnig.myexpenses
 * This class runs first and tests if the database is initialized
 * welcome dialog shown, and the menu built up
 * 
 * @author Michael Totschnig
 */
public class A_InstallTest extends ActivityInstrumentationTestCase2<MyExpenses> {

  private Activity mActivity;
  private Solo mSolo;
  private Instrumentation mInstrumentation;
  private Context mContext;
  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public A_InstallTest() {
    super(MyExpenses.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    mInstrumentation = getInstrumentation();
    mContext = mInstrumentation.getTargetContext();
    Fixture.clear(mContext);
    setActivityInitialTouchMode(false);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
  }
  public void testDatabaseIsCreatedAndWelcomeDialogIsShown() {
    assertTrue("Welcome Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_welcome)));
    assertTrue("Close button not show",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    mSolo.clickOnButton(mContext.getString(android.R.string.ok));
    assertTrue("Empty view not shown", mSolo.searchText(mContext.getString(R.string.no_expenses)));
    mAdapter = (FragmentPagerAdapter) mPager.getAdapter();
    assertTrue(mAdapter != null);
    assertEquals(mAdapter.getCount(),1);
  }
  @Override
  public void tearDown() throws Exception {
    mSolo.finishOpenedActivities();
  }
}
