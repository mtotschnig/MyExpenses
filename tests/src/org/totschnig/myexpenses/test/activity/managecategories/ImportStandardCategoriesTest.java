package org.totschnig.myexpenses.test.activity.managecategories;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;

import android.widget.ListView;

import com.robotium.solo.Solo;

public class ImportStandardCategoriesTest extends MyActivityTest<ManageCategories> {
  
  private ListView mList;

  public ImportStandardCategoriesTest() {
    super(ManageCategories.class);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(mInstrumentation, mActivity);
    mSolo.waitForActivity(ManageCategories.class);
    mList = (ListView) mActivity.getListFragment().getView().findViewById(R.id.list);
  }
  public void testImportDefault() {
    //we run on a cleared database
    int itemsInList = mList.getAdapter().getCount();
    assertEquals(0,itemsInList);
    mSolo.clickOnButton(mContext.getString(R.string.menu_categories_setup_default));
    assertTrue(mSolo.waitForDialogToClose());
    itemsInList = mList.getAdapter().getCount();
    assertTrue(itemsInList>0);
  }
}
