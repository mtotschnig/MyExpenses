package org.totschnig.myexpenses.test.activity.myexpenses_context;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Intent;

import com.robotium.solo.Solo;

public class D_ContextActionTest extends MyActivityTest<MyExpenses> {
  private StickyListHeadersListView mList;

  public D_ContextActionTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(mInstrumentation, mActivity);
    
    Fixture.setup(mInstrumentation, Locale.getDefault(), Currency.getInstance("USD"),1);
    setActivity(null);
    setActivityInitialTouchMode(false);
    long accountId = Fixture.getAccount1().id;
    Intent i = new Intent()
      .putExtra(KEY_ROWID,accountId)
      .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses")
      ;
    setActivityIntent(i);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mSolo.waitForActivity(MyExpenses.class);
    mList =  (StickyListHeadersListView) ((MyExpenses) mActivity).getCurrentFragment().getView().findViewById(R.id.list);
  }
  public void testA_Clone() {
    int itemsInList = mList.getAdapter().getCount();
    setSelection();
    mInstrumentation.waitForIdleSync();
    invokeContextAction("CLONE_TRANSACTION");
    mInstrumentation.waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList+1, mList.getAdapter().getCount());
  }
  public void testB_Edit() {
    setSelection();
    mInstrumentation.waitForIdleSync();
    invokeContextAction("EDIT");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }

  public void testC_CreateTemplate() {
    String templateTitle = "Robotium Template Test";
    setSelection();
    mInstrumentation.waitForIdleSync();
    invokeContextAction("CREATE_TEMPLATE");
    assertTrue("Edit Title dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_template_title)));
    mSolo.enterText(0, templateTitle);
    mSolo.sendKey(Solo.ENTER);
    //((EditText) mSolo.getView(EditText.class, 0)).onEditorAction(EditorInfo.IME_ACTION_DONE);
    clickOnActionBarItem("MANAGE_PLANS");
    assertTrue(mSolo.waitForActivity(ManageTemplates.class.getSimpleName()));
    assertTrue(mSolo.searchText(templateTitle));
  }
  public void testD_Delete() {
    int itemsInList = mList.getAdapter().getCount();
    setSelection();
    mInstrumentation.waitForIdleSync();
    invokeContextAction("DELETE");
    assertTrue("Delete confirmation not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_warning_delete_transaction)));
    mSolo.clickOnButton(mContext.getString(R.string.menu_delete));
    mInstrumentation.waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList-1, mList.getAdapter().getCount());
  }
  private void setSelection() {
    mActivity
    .runOnUiThread(new Runnable() {
      public void run() { 
        mList.requestFocus();
        mList.getWrappedList().setSelection(3);
      }
      });
  }
  private void sleep() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
