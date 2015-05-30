package org.totschnig.myexpenses.test.activity.myexpensescontext;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.test.activity.MyExpensesTest;
import org.totschnig.myexpenses.test.util.Fixture;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.robotium.solo.Solo;

public class D_ContextActionTest extends MyExpensesTest {
  Account account1;

  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    
    setup(Locale.getDefault(), Currency.getInstance("USD"));
    setActivity(null);
    setActivityInitialTouchMode(false);
    Intent i = new Intent()
      .putExtra(KEY_ROWID,account1.getId())
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
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
    clickOnActionBarItem("SAVE");
    getInstrumentation().waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList + 1, requireList().getAdapter().getCount());
  }
  public void testB_Edit() {
    int itemsInList = requireList().getAdapter().getCount();
    setSelection();
    invokeContextAction("EDIT");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
    clickOnActionBarItem("SAVE");
    getInstrumentation().waitForIdleSync();
    //wait for adapter to have updated
    sleep();
    assertEquals(itemsInList, requireList().getAdapter().getCount());
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
    getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
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
  public void setup(Locale locale, Currency defaultCurrency) {
    Context testContext = getInstrumentation().getContext();
    Context appContext = getInstrumentation().getTargetContext().getApplicationContext();
    Fixture.setUpCategories(locale,appContext);

    account1 = new Account("TEST",0,"D_ContextActionTest");
    account1.save();
    Transaction op0 = Transaction.getNewInstance(account1.getId());
    op0.amount = new Money(defaultCurrency,-1200L);
    op0.save();
    op0.saveAsNew();
    op0.saveAsNew();
    op0.saveAsNew();
    op0.saveAsNew();
    op0.saveAsNew();
  }

  @Override
  public void tearDown() throws Exception {
    Account.delete(account1.getId());
    super.tearDown();
  }
}
