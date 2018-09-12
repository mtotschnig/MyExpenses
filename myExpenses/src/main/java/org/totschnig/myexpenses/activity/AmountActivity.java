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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.AmountEditText;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.widget.AbstractWidget;

import java.math.BigDecimal;

import butterknife.BindView;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public abstract class AmountActivity extends EditActivity {
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
      try {
        View target = findViewById(intent.getIntExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, 0));
        AmountEditText input;
        if (target instanceof AmountEditText) {
          input = (AmountEditText) target;
        } else if (target instanceof AmountInput) {
          input = ((AmountInput) target).getAmountEditText();
        } else {
          throw new IllegalStateException("CALCULATOR_REQUEST launched with incorrect EXTRA_KEY_INPUT_ID");
        }
        input.setAmount(new BigDecimal(intent.getStringExtra(KEY_AMOUNT)));
        input.setError(null);
      } catch (Exception e) {
        CrashHandler.report(e);
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
    return amountInput.getTypedValue(true, showToUser);
  }

  protected BigDecimal validateAmountInput(AmountEditText input, boolean showToUser) {
    return input.validate(showToUser);
  }

  public void showCalculator(View view) {
    ViewGroup row = (ViewGroup) view.getParent();
    if (row instanceof AmountInput) {
      showCalculatorInternal(((AmountInput) row).validate(false), row.getId());
    } else {
      for (int itemPos = 0; itemPos < row.getChildCount(); itemPos++) {
        View input = row.getChildAt(itemPos);
        if (input instanceof AmountEditText) {
          showCalculatorInternal(((AmountEditText) input).validate(false), input.getId());
          break;
        }
      }
    }
  }

  private void showCalculatorInternal(BigDecimal amount, int id) {
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