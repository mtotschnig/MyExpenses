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
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.ui.ExchangeRateEdit;

import java.math.BigDecimal;

import butterknife.BindView;

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