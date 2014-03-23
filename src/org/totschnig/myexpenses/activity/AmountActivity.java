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
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public abstract class AmountActivity extends EditActivity {
  private static final int CALCULATOR_REQUEST = 0;
  protected DecimalFormat nfDLocal;
  protected EditText mAmountText;
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  protected boolean mType = EXPENSE;
  protected Button mTypeButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
  }

  /**
   * configures the decimal format and the amount EditText based on configured
   * currency_decimal_separator 
   */
  protected void configAmountInput() {
    mAmountText = (EditText) findViewById(R.id.Amount);
    final char decimalSeparator = Utils.getDefaultDecimalSeparator();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    final char otherSeparator = decimalSeparator == '.' ? ',' : '.';
    nfDLocal = new DecimalFormat("#0.###",symbols);
    //mAmountText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
    //due to bug in Android platform http://code.google.com/p/android/issues/detail?id=2626
    //the soft keyboard if it occupies full screen in horizontal orientation does not display
    //the , as comma separator
    mAmountText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    mAmountText.setFilters(new InputFilter[] { new InputFilter() {
      public CharSequence filter(CharSequence source, int start, int end,
          Spanned dest, int dstart, int dend) {
        boolean separatorPresent = dest.toString().indexOf(decimalSeparator) > -1;
        for (int i = start; i < end; i++) {
          if (source.charAt(i) == otherSeparator || source.charAt(i) == decimalSeparator) {
            char[] v = new char[end - start];
            TextUtils.getChars(source, start, end, v, 0);
            String s = new String(v).replace(otherSeparator,decimalSeparator);
            if (separatorPresent)
              return s.replace(String.valueOf(decimalSeparator),"");
            else
              return s;
            }
          }
        return null; // keep original
      }
    }});
    nfDLocal.setGroupingUsed(false);
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST) {
      try {
        mAmountText.setText(nfDLocal.format(new BigDecimal(intent.getStringExtra(KEY_AMOUNT))));
        mAmountText.setError(null);
      } catch (NumberFormatException  e) {}
        catch (IllegalArgumentException e) {}
    }
  }

  protected void configureType() {
    mTypeButton.setText(mType ? "+" : "-");
  }

  protected BigDecimal validateAmountInput(boolean showToUser) {
    String strAmount = mAmountText.getText().toString();
    if (strAmount.equals("")) {
      if (showToUser)
        mAmountText.setError(getString(R.string.no_amount_given));
      return null;
    }
    BigDecimal amount = Utils.validateNumber(nfDLocal, strAmount);
    if (amount == null) {
      if (showToUser)
        mAmountText.setError(getString(R.string.invalid_number_format,nfDLocal.format(11.11)));
      return null;
    }
    return amount;
  }
  public void showCalculator(View view) {
    Intent intent = new Intent(AmountActivity.this,CalculatorInput.class);
    intent.putExtra(KEY_AMOUNT,mAmountText.getText().toString());
    startActivityForResult(intent, CALCULATOR_REQUEST);
  }
}