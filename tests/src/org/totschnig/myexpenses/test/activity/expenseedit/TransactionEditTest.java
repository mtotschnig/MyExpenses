package org.totschnig.myexpenses.test.activity.expenseedit;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import android.content.Intent;

import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import static  org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

public class TransactionEditTest extends MyActivityTest<ExpenseEdit> {

  public TransactionEditTest() {
    super(ExpenseEdit.class,true);
  }
  public void setUp() throws Exception {
    super.setUp();
    //mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
    Fixture.setup(mInstrumentation, new Locale("en","US"), Currency.getInstance("USD"));
  }
  public void testTransaction() {
    setActivity(null);
    //we assume that MyExpenses has set up the default account with id 1
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, 1L);
    setActivityIntent(i);
    mActivity = getActivity();
    assertTrue(mSolo.searchText(mContext.getString(R.string.date),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.time),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.amount),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.comment),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.category),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.payee),true));
  }
  public void testTransfer() {
    setActivity(null);
    //we assume that MyExpenses has set up the default account with id 1
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSFER);
    i.putExtra(KEY_ACCOUNTID, 1L);
    setActivityIntent(i);
    mActivity = getActivity();
    assertTrue(mSolo.searchText(mContext.getString(R.string.date),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.time),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.amount),true));
    assertTrue(mSolo.searchText(mContext.getString(R.string.comment),true));
  }
}
