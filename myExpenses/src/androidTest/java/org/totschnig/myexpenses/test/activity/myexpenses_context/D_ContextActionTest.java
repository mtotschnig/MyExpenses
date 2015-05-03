package org.totschnig.myexpenses.test.activity.myexpenses_context;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.activity.MyExpensesTest;
import org.totschnig.myexpenses.test.util.Fixture;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Intent;

import com.robotium.solo.Solo;

public class D_ContextActionTest extends MyExpensesTest {

  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    
    Fixture.setup(getInstrumentation(), Locale.getDefault(), Currency.getInstance("USD"),1);
    setActivity(null);
    setActivityInitialTouchMode(false);
    long accountId = Fixture.getAccount1().getId();
    Intent i = new Intent()
      .putExtra(KEY_ROWID,accountId)
      .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses")
      ;
    setActivityIntent(i);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mSolo.waitForActivity(MyExpenses.class);
  }
  public void testA_Clone() {
    int itemsInList = requireList().getAdapter().getCount();
    setSelection();
    invokeContextAction("CLONE_TRANSACTION");
    getInstrumentation().waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList + 1, requireList().getAdapter().getCount());
  }
  public void testB_Edit() {
    setSelection();
    invokeContextAction("EDIT");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }

  public void testC_CreateTemplate() {
    String templateTitle = "Robotium Template Test";
    setSelection();
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
    int itemsInList = requireList().getAdapter().getCount();
    setSelection();
    invokeContextAction("DELETE");
    assertTrue("Delete confirmation not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_warning_delete_transaction)));
    mSolo.clickOnButton(mContext.getString(R.string.menu_delete));
    getInstrumentation().waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList - 1, requireList().getAdapter().getCount());
  }
  public void testE_Split() {
    setSelection();
    invokeContextAction("SPLIT_TRANSACTION");
    if (!MyApplication.getInstance().isContribEnabled()) {
      assertTrue("Contrib Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_contrib_feature)));
      mSolo.clickOnText(mContext.getString(R.string.dialog_contrib_no));
    }
    getInstrumentation().waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertTrue("Split transaction without effect", mSolo.searchText(mContext.getString(R.string.split_transaction)));
    
  }
  private void setSelection() {
    final StickyListHeadersListView listView = requireList();
    mActivity
    .runOnUiThread(new Runnable() {
      public void run() {
        listView.requestFocus();
        listView.getWrappedList().setSelection(3);
      }
    });
    getInstrumentation().waitForIdleSync();
    sleep();
  }

}
