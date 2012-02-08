package org.totschnig.myexpenses.test;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.MyExpenses;

import com.jayway.android.robotium.solo.Solo;

import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListAdapter;

/**
 * For the moment just an experiment in how to test the UI
 * http://robotium.googlecode.com/files/robotium-solo-3.1.jar
 * @author Michael Totschnig
 *
 */
public class MyExpensesTest extends
  ActivityInstrumentationTestCase2 <MyExpenses> {
  private MyExpenses mActivity;
  private ListAdapter mAdapter;
  private Instrumentation mInstrumentation = null;
  private Solo solo;

  

  public MyExpensesTest() {
    super("org.totschnig.myexpenses",MyExpenses.class);
  }
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    MyApplication app = (MyApplication) getInstrumentation().getTargetContext().getApplicationContext();
    app.setSettings(app.getSharedPreferences("functest",Context.MODE_PRIVATE));
    app.setDatabaseName("functest");
    setActivityInitialTouchMode(false); 
    mActivity = getActivity();
    mInstrumentation = getInstrumentation();
  } // end of setUp() method definition
  
  public void testMainScreen() {
    mAdapter = mActivity.getListAdapter();
    assertTrue(mAdapter != null);
    assertEquals(0,mAdapter.getCount());
    mInstrumentation.invokeMenuActionSync(mActivity, MyExpenses.RESET_ID, 0);
    solo = new Solo(getInstrumentation(), getActivity());
    getInstrumentation().waitForIdleSync();
    assertTrue(solo.searchText("will be deleted"));
  }
}
