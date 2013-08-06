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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

public abstract class EditActivity extends ProtectedFragmentActivity {
  public static final String CURRENCY_USE_MINOR_UNIT = "x";
  private static final int CALCULATOR_REQUEST = 0;
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
    //not needed in HOLO
    if (Build.VERSION.SDK_INT > 10)
      return;
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
      mAmountText.setKeyListener(DigitsKeyListener.getInstance("0123456789"+mCurrencyDecimalSeparator));
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
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.one, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case R.id.Confirm:
      if (saveState()) {
        setResult(RESULT_OK);
        finish();
      }
      return true;
    }
    return super.dispatchCommand(command, tag);
  }
  protected BigDecimal validateAmountInput(boolean showToUser) {
    String strAmount = mAmountText.getText().toString();
    if (strAmount.equals("")) {
      if (showToUser)
        Toast.makeText(this,getString(R.string.no_amount_given), Toast.LENGTH_LONG).show();
      return null;
    }
    BigDecimal amount = Utils.validateNumber(nfDLocal, strAmount);
    if (amount == null) {
      if (showToUser)
        Toast.makeText(this,getString(R.string.invalid_number_format,nfDLocal.format(11.11)), Toast.LENGTH_LONG).show();
      return null;
    }
    return amount;
  }
  abstract protected boolean saveState();
}