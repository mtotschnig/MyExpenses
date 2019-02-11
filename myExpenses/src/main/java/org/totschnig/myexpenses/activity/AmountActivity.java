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
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.widget.AbstractWidget;

import java.math.BigDecimal;

import butterknife.BindView;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public abstract class AmountActivity extends EditActivity implements AmountInput.Host {
  @BindView(R.id.AmountLabel)
  protected TextView mAmountLabel;
  @BindView(R.id.AmountRow)
  ViewGroup amountRow;
  @BindView(R.id.ExchangeRateRow)
  ViewGroup exchangeRateRow;
  @BindView(R.id.Amount)
  AmountInput amountInput;
  @BindView(R.id.ExchangeRate)
  ExchangeRateEdit mExchangeRateEdit;

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST && intent != null) {
      View target = findViewById(intent.getIntExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, 0));
      if (target instanceof AmountInput) {
        ((AmountInput) target).setAmount(new BigDecimal(intent.getStringExtra(KEY_AMOUNT)), false);
      } else {
        showSnackbar("CALCULATOR_REQUEST launched with incorrect EXTRA_KEY_INPUT_ID", Snackbar.LENGTH_LONG);
      }
    }
  }

  /**
   * @return true for income, false for expense
   */
  protected boolean isIncome() {
    return amountInput.getType();
  }

  protected void onTypeChanged(boolean isChecked) {
    setDirty();
    configureType();
  }

  protected void configureType() {}

  protected BigDecimal validateAmountInput(boolean showToUser) {
    return validateAmountInput(amountInput, showToUser);
  }

  protected BigDecimal validateAmountInput(AmountInput input, boolean showToUser) {
    return input.getTypedValue(true, showToUser);
  }

  @Override
  public void showCalculator(BigDecimal amount, int id) {
    Intent intent = new Intent(this, CalculatorInput.class);
    forwardDataEntryFromWidget(intent);
    if (amount != null) {
      intent.putExtra(KEY_AMOUNT, amount);
    }
    intent.putExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, id);
    startActivityForResult(intent, CALCULATOR_REQUEST);
  }

  protected void forwardDataEntryFromWidget(Intent intent) {
    intent.putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY,
        getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false));
  }

  protected void setupListeners() {
    amountInput.addTextChangedListener(this);
    amountInput.setTypeChangedListener(this::onTypeChanged);
  }

  protected void linkInputsWithLabels() {
    linkInputWithLabel(amountInput, mAmountLabel);
    linkInputWithLabel(amountRow.findViewById(R.id.Calculator), mAmountLabel);
    final View exchangeRateLabel = findViewById(R.id.ExchangeRateLabel);
    linkInputWithLabel(mExchangeRateEdit, exchangeRateLabel);
  }
}