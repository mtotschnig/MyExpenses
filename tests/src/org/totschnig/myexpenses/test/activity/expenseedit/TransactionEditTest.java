package org.totschnig.myexpenses.test.activity.expenseedit;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;

import android.content.Intent;

import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import static  org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

public class TransactionEditTest extends MyActivityTest<ExpenseEdit> {

  public TransactionEditTest() {
    super(ExpenseEdit.class);
  }
  public void setUp() throws Exception {
    super.setUp();
    //we assume that MyExpenses has set up the default account with id 1
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, 1L);
    setActivityIntent(i);
    mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);    
  }
  public void testFormLabels() {
    mSolo.searchText(mContext.getString(R.string.date));
    mSolo.searchText(mContext.getString(R.string.time));
    mSolo.searchText(mContext.getString(R.string.amount));
    mSolo.searchText(mContext.getString(R.string.comment));
    mSolo.searchText(mContext.getString(R.string.category));
    mSolo.searchText(mContext.getString(R.string.payee));
  }
}
