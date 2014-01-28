package org.totschnig.myexpenses.test.activity.myexpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Intent;
import android.os.Build;
import android.test.UiThreadTest;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;

import com.robotium.solo.Solo;

public class D_ContextActionTest extends MyActivityTest<MyExpenses> {

  public D_ContextActionTest() {
    super(MyExpenses.class,true);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(mInstrumentation, mActivity);
    
    Fixture.setup(mInstrumentation, Locale.getDefault(), Currency.getInstance("USD"));
    setActivity(null);
    setActivityInitialTouchMode(false);
    long accountId = Fixture.getAccount3().id;
    Intent i = new Intent()
      .putExtra(KEY_ROWID,accountId)
      .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses")
      ;
    setActivityIntent(i);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mSolo.waitForActivity(MyExpenses.class);
  }
  public void testContextualActions() {
    final StickyListHeadersListView l =  (StickyListHeadersListView) mActivity.findViewById(R.id.list);
    int itemsInList = l.getAdapter().getCount();
    Log.i("DEBUG", "count of items lin list " + itemsInList);
    mActivity
    .runOnUiThread(new Runnable() {
      public void run() { 
        l.requestFocus();
        l.setSelection(0);
      }
      });
    invokeContextAction("DELETE");
    assertTrue("Delete confirmation not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_warning_delete_transaction)));
    mSolo.clickOnButton(mContext.getString(R.string.menu_delete));
    mInstrumentation.waitForIdleSync();
    //wait for adapter to have updated
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(itemsInList-1, l.getAdapter().getCount());
  }
}
