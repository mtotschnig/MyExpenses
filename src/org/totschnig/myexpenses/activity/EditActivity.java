/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.Utils;
import org.totschnig.myexpenses.R.color;
import org.totschnig.myexpenses.R.id;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public abstract class EditActivity extends ProtectedActivity {
  public static final String CURRENCY_USE_MINOR_UNIT = "x";
  private static final int CALCULATOR_REQUEST = 1;
  protected DecimalFormat nfDLocal;
  protected String mCurrencyDecimalSeparator;
  protected boolean mMinorUnitP;
  protected EditText mAmountText;
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  protected boolean mType = EXPENSE;

  public EditActivity() {
    super();
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
  }

  protected void changeEditTextBackground(ViewGroup root) {
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    if (settings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("dark")) {
      int c = getResources().getColor(R.color.theme_dark_button_color);
      for(int i = 0; i <root.getChildCount(); i++) {
        View v = root.getChildAt(i);
        if(v instanceof EditText) {
          Utils.setBackgroundFilter(v, c);
        } else if(v instanceof ViewGroup) {
          changeEditTextBackground((ViewGroup)v);
        }
      }
    }
  }
  /**
   * configures the decimal format and the amount EditText based on configured
   * currency_decimal_separator 
   */
  protected void configAmountInput() {
    mAmountText = (EditText) findViewById(R.id.Amount);
    SharedPreferences settings = MyApplication.getInstance().getSettings();
    mCurrencyDecimalSeparator = settings.getString(MyApplication.PREFKEY_CURRENCY_DECIMAL_SEPARATOR,
        Utils.getDefaultDecimalSeparator());
    mMinorUnitP = mCurrencyDecimalSeparator.equals(CURRENCY_USE_MINOR_UNIT);
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
      mAmountText.setKeyListener(DigitsKeyListener.getInstance("+-0123456789"+mCurrencyDecimalSeparator));
      mAmountText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
      mAmountText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    }
    nfDLocal.setGroupingUsed(false);
    findViewById(R.id.calculator).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(EditActivity.this,CalculatorInput.class);
        intent.putExtra(MyApplication.EXTRA_AMOUNT,mAmountText.getText().toString());
        startActivityForResult(intent, CALCULATOR_REQUEST);
      }
    });
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST) {
      mAmountText.setText(nfDLocal.format(new BigDecimal(intent.getStringExtra(MyApplication.EXTRA_AMOUNT))));
    }
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("type", mType);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mType = savedInstanceState.getBoolean("type");
  }
}
