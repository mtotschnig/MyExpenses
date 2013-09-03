package org.totschnig.myexpenses.test.activity;

import java.util.Locale;

import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;
import com.jayway.android.robotium.solo.Solo;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;

public class TestMain extends ActivityInstrumentationTestCase2<MyExpenses> {
	private Solo solo;
	private MyApplication app;
	
	public TestMain() {
		super(MyExpenses.class);
	}
	
	@Override
	protected void setUp() throws Exception {
	  app = (MyApplication) getInstrumentation().getTargetContext().getApplicationContext(); 
		super.setUp();
	}
	public void testLangEn() {
	  helperTestLang("en");
	}
  public void testLangFr() {
    helperTestLang("fr");
  }
  public void testLangDe() {
    helperTestLang("de");
  }
  public void testLangIt() {
    helperTestLang("it");
  }
  public void testLangEs() {
    helperTestLang("es");
  }
  public void testLangTr() {
    helperTestLang("tr");
  }
  public void testLangVi() {
    helperTestLang("vi");
  }
	private void helperTestLang(String lang) {
	  solo = new Solo(getInstrumentation(), getActivity());
	  Locale locale = new Locale(lang);  
	  Locale.setDefault(locale); 
	  Configuration config = new Configuration(); 
	  config.locale = locale; 
	  app.getResources().updateConfiguration(config,  
	      app.getResources().getDisplayMetrics());
	  helperDbFixture();
    assertTrue(solo.searchText(getActivity().getString(android.R.string.ok)));
    solo.takeScreenshot(lang);
	}
	private void helperDbFixture() {
    Account account = new Account(
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account1Label),
        200,
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account1Description)
    );
    account.save();
    account = new Account(
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account2Label),
        200,
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account2Description)
    );
    account.save();
    account = new Account(
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account3Label),
        200,
        solo.getString(org.totschnig.myexpenses.test.R.string.testData_account3Description)
    );
    account.save();
	}
	@Override
	protected void tearDown() throws Exception{
			solo.finishOpenedActivities();
	}
}