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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.widget.AbstractWidget;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public abstract class AmountActivity extends EditActivity {
  protected DecimalFormat nfDLocal;
  protected AmountEditText mAmountText;
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  protected boolean mType = EXPENSE;
  protected CompoundButton mTypeButton;
  protected TextView mAmountLabel;

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    mAmountLabel = (TextView) findViewById(R.id.AmountLabel);
    mAmountText = (AmountEditText) findViewById(R.id.AmountRow).findViewById(R.id.Amount);
  }

  /**
   * 
   */
  protected void configTypeButton() {
    mTypeButton = (CompoundButton) findViewById(R.id.AmountRow).findViewById(R.id.TaType);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST && intent != null) {
      try {
        AmountEditText input = (AmountEditText)
            findViewById(intent.getIntExtra(CalculatorInput.EXTRA_KEY_INPUT_ID,0));
        input.setAmount(new BigDecimal(intent.getStringExtra(KEY_AMOUNT)));
        input.setError(null);
      } catch (Exception  e) {
        AcraHelper.report(e);
      }
    }
  }
  protected void onTypeChanged(boolean isChecked) {
    mType = isChecked;
    setDirty(true);
    configureType();
  }

  protected void configureType() {
    mTypeButton.setChecked(mType);
    mTypeButton.setContentDescription(getString(
        mType ? R.string.pm_type_credit : R.string.pm_type_debit));
  }

  protected BigDecimal validateAmountInput(boolean showToUser) {
    return validateAmountInput(mAmountText, showToUser);
  }

  protected BigDecimal validateAmountInput(AmountEditText input, boolean showToUser) {
    return input.validate(showToUser);
  }

  public void showCalculator(View view) {
    showCalculatorInternal(mAmountText);
  }

  protected void showCalculatorInternal(AmountEditText input) {
    Intent intent = new Intent(this,CalculatorInput.class);
    forwardDataEntryFromWidget(intent);
    BigDecimal amount = validateAmountInput(input, false);
    if (amount!=null) {
      intent.putExtra(KEY_AMOUNT,amount);
    }
    intent.putExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, input.getId());
    startActivityForResult(intent, CALCULATOR_REQUEST);
  }

  protected void forwardDataEntryFromWidget(Intent intent) {
    intent.putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY,
        getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false));
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
    configureType();
  }
  protected void setupListeners() {
    mAmountText.addTextChangedListener(this);
    mTypeButton.setOnCheckedChangeListener((buttonView, isChecked) -> onTypeChanged(isChecked));
  }
  protected void linkInputsWithLabels() {
    linkInputWithLabel(mAmountText, mAmountLabel);
    linkInputWithLabel(mTypeButton, mAmountLabel);
    linkInputWithLabel(findViewById(R.id.AmountRow).findViewById(R.id.Calculator),mAmountLabel);
  }
}