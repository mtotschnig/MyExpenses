package org.totschnig.myexpenses;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public abstract class EditActivity extends Activity {
  protected DecimalFormat nfDLocal;
  protected String mCurrencyDecimalSeparator;
  protected boolean mMinorUnitP;
  protected EditText mAmountText;

  public EditActivity() {
    super();
  }

  /**
   * configures the decimal format and the amount EditText based on configured
   * currency_decimal_separator 
   */
  protected void configAmountInput() {
    mAmountText = (EditText) findViewById(R.id.Amount);
    SharedPreferences settings = ((MyApplication) getApplicationContext()).getSettings();
    mCurrencyDecimalSeparator = settings.getString("currency_decimal_separator", Utils.getDefaultDecimalSeparator());
    mMinorUnitP = mCurrencyDecimalSeparator.equals(MyApplication.CURRENCY_USE_MINOR_UNIT);
    if (mMinorUnitP) {
      nfDLocal = new DecimalFormat("#0");
      nfDLocal.setParseIntegerOnly(true);
      mAmountText.setInputType(InputType.TYPE_CLASS_NUMBER);
    } else {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setDecimalSeparator(mCurrencyDecimalSeparator.charAt(0));
      nfDLocal = new DecimalFormat("#0.###",symbols);

      //mAmountText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
      //due to bug in Android platform http://code.google.com/p/android/issues/detail?id=2626
      //the soft keyboard if it occupies full screen in horizontal orientation does not display
      //the , as comma separator
      mAmountText.setKeyListener(DigitsKeyListener.getInstance("0123456789"+mCurrencyDecimalSeparator));
      mAmountText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
      mAmountText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    }
    nfDLocal.setGroupingUsed(false);
  }
  
}
