package org.totschnig.myexpenses.test.activity.expenseedit;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import android.content.Intent;

import com.robotium.solo.Solo;

import static  org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;

public class TransactionEditTest extends MyActivityTest<ExpenseEdit> {

  public TransactionEditTest() {
    super(ExpenseEdit.class,false);
  }
  public void setUp() throws Exception {
    super.setUp();
    //mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    Fixture.setup(mInstrumentation, Locale.getDefault(), Currency.getInstance("USD"));
  }
  public void testTransaction() {
    setActivity(null);
    //we assume that Fixture has set up the default account with id 1
    Intent i = new Intent(Intent.ACTION_EDIT);
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra("operationType", MyExpenses.TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, 1L);
    setActivityIntent(i);
    mActivity = getActivity();
    assertTrue("Date is not shown",mSolo.searchText(mContext.getString(R.string.date),true));
    assertTrue("Time is not shown",mSolo.searchText(mContext.getString(R.string.time),true));
    assertTrue("Amount is not shown",mSolo.searchText(mContext.getString(R.string.amount),true));
    assertTrue("Comment is not shown",mSolo.searchText(mContext.getString(R.string.comment),true));
    assertTrue("Category is not shown",mSolo.searchText(mContext.getString(R.string.category),true));
    assertTrue("Payee is not shown",mSolo.searchText(mContext.getString(R.string.payee),true));
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
    assertTrue("Date is not shown",mSolo.searchText(mContext.getString(R.string.date),true));
    assertTrue("Time is not shown",mSolo.searchText(mContext.getString(R.string.time),true));
    assertTrue("Amount is not shown",mSolo.searchText(mContext.getString(R.string.amount),true));
    assertTrue("Comment is not shown",mSolo.searchText(mContext.getString(R.string.comment),true));
  }
}
