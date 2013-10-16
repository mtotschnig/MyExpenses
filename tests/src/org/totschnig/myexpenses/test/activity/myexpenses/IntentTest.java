package org.totschnig.myexpenses.test.activity.myexpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.test.util.Fixture;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;


/**
 * We test that starting activity with account_id will
 * display the intended account
 * 
 * @author Michael Totschnig
 */
public class IntentTest extends ActivityInstrumentationTestCase2<MyExpenses> {

  private Activity mActivity;
  private Solo mSolo;
  private Instrumentation mInstrumentation;
  private Context mContext;
  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public IntentTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mInstrumentation = getInstrumentation();
    Fixture.clear(mInstrumentation);
    mActivity = getActivity();
    Fixture.setup(mInstrumentation, new Locale("en","US"), Currency.getInstance("USD"));
//    setActivityInitialTouchMode(false);
//    mSolo = new Solo(getInstrumentation(), mActivity);
//    mInstrumentation = getInstrumentation();
//    mContext = mInstrumentation.getTargetContext();
//    mPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
  }
  public void testNavigateToAccountReceivedThroughIntent() {
    setActivity(null);
    Cursor cursor = mInstrumentation.getContext().getContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        new String[] {KEY_ROWID,KEY_LABEL},                       // no projection, get all columns
        null,                       // no selection criteria, get all records
        null,                       // no selection arguments
        null                        // use default sort order
    );
    cursor.moveToFirst();
    while(!cursor.isAfterLast()) {
      setActivityInitialTouchMode(false);
      Intent i = new Intent()
        .putExtra(KEY_ROWID, cursor.getLong(cursor.getColumnIndex(KEY_ROWID)))
        .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses");
      setActivityIntent(i);
      mActivity = getActivity();
      mSolo = new Solo(getInstrumentation(), mActivity);
      mSolo.searchText(cursor.getString(cursor.getColumnIndex(KEY_LABEL)));
      mActivity.finish();  // close the activity
      setActivity(null);
      cursor.moveToNext();
    }
    cursor.close();
  }
  @Override
  public void tearDown() throws Exception {
    mSolo.finishOpenedActivities();
  }
}
