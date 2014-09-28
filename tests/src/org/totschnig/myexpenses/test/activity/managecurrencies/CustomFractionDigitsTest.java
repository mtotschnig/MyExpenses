package org.totschnig.myexpenses.test.activity.managecurrencies;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Account.CurrencyEnum;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
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
    String currencyToTest = CurrencyEnum.values()[0].name();
    //we start from 2
    SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
        .putInt(currencyToTest+Money.KEY_CUSTOM_FRACTION_DIGITS,2));
    Fixture.setup(mInstrumentation, Locale.getDefault(),
        Currency.getInstance(currencyToTest),1);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mSolo.waitForActivity(ManageCurrencies.class);
  }
  public void testChangeOfFractionDigitsShouldKeepTransactionSum() {
    Account account = Fixture.getAccount1();
    Money before = account.getTotalBalance();
    mInstrumentation.waitForIdleSync();
    mSolo.clickInList(0);
    assertTrue(mSolo.waitForText(mActivity.getString(R.string.dialog_title_set_fraction_digits)));
    mSolo.clearEditText(0);
    mSolo.enterText(0,"3");
    mSolo.sendKey(Solo.ENTER);
    mSolo.waitForDialogToClose();
    //refetch account
    account = Account.getInstanceFromDb(account.getId());
    Money after = account.getTotalBalance();
    assertEquals(before.getAmountMajor(),after.getAmountMajor());
    assertEquals(before.getAmountMinor()*10,after.getAmountMinor().longValue());
  }
}
