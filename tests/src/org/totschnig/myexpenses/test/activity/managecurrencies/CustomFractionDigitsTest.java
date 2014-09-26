package org.totschnig.myexpenses.test.activity.managecurrencies;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.CurrencyEnum;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import android.content.Intent;
import android.widget.ListView;

import com.robotium.solo.Solo;

public class CustomFractionDigitsTest extends MyActivityTest<ManageCurrencies> {
  private ListView mList;

  public CustomFractionDigitsTest() {
    super(ManageCurrencies.class);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(mInstrumentation, mActivity);
    
    Fixture.setup(mInstrumentation, Locale.getDefault(),
        Currency.getInstance(CurrencyEnum.values()[0].name()),1);
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
    mSolo.waitForActivity(ManageCurrencies.class);
    mList =  (ListView) ((ManageCurrencies) mActivity)
        .getSupportFragmentManager().findFragmentById(R.id.currency_list).getView().findViewById(R.id.list);
  }
  public void testChangeOfFractionDigitsShouldKeepTransactionSum() {
    Account account = Fixture.getAccount1();
    BigDecimal before = account.getTotalBalance().getAmountMajor();
    mSolo.clickInList(0);
    assertTrue(mSolo.waitForText(mActivity.getString(R.string.dialog_title_set_fraction_digits)));
    mSolo.enterText(0,"8");
    mSolo.sendKey(Solo.ENTER);
    mInstrumentation.waitForIdleSync();
    assertEquals(before,account.getTotalBalance().getAmountMajor());
  }
}
