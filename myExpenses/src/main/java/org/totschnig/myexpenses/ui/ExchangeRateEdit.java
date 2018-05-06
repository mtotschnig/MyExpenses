package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

import java.math.BigDecimal;
import java.math.RoundingMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExchangeRateEdit extends LinearLayout {

  public interface ExchangeRateWatcher {
    void afterExchangeRateChanged(BigDecimal rate, BigDecimal inverse);
  }

  private static final int EXCHANGE_RATE_FRACTION_DIGITS = 5;
  private static final BigDecimal nullValue = new BigDecimal(0);

  @BindView(R.id.ExchangeRate_1)
  ViewGroup rate1Container;
  AmountEditText rate1Edit;
  @BindView(R.id.ExchangeRate_2)
  ViewGroup rate2Container;
  AmountEditText rate2Edit;

  boolean blockWatcher = false;

  public void setExchangeRateWatcher(ExchangeRateWatcher exchangeRateWatcher) {
    this.exchangeRateWatcher = exchangeRateWatcher;
  }

  public void setBlockWatcher(boolean blockWatcher) {
    this.blockWatcher = blockWatcher;
  }

  private ExchangeRateWatcher exchangeRateWatcher;

  public ExchangeRateEdit(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.exchange_rates, this, true);
    ButterKnife.bind(this);
    rate1Edit = rate1Container.findViewById(R.id.ExchangeRateText);
    rate1Edit.setId(R.id.ExchangeRateEdit1);
    rate2Edit = rate2Container.findViewById(R.id.ExchangeRateText);
    rate2Edit.setId(R.id.ExchangeRateEdit2);
    rate1Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate2Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate1Edit.addTextChangedListener(new LinkedExchangeRateTextWatchter(true  ));
    rate2Edit.addTextChangedListener(new LinkedExchangeRateTextWatchter(false));
  }

  /**
   * does not trigger call to registered ExchangeRateWatcher calculates rates based on two values
   * @param amount1
   * @param amount2
   */
  public void calculateAndSetRate(@Nullable BigDecimal amount1, @Nullable BigDecimal amount2) {
    blockWatcher = true;
    BigDecimal exchangeRate = (amount1 != null && amount2 != null && amount1.compareTo(nullValue) != 0) ?
        amount2.divide(amount1, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) : nullValue;
    BigDecimal inverseExchangeRate = (amount1 != null && amount2 != null && amount2.compareTo(nullValue) != 0) ?
            amount1.divide(amount2, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) : nullValue;
    rate1Edit.setAmount(exchangeRate);
    rate2Edit.setAmount(inverseExchangeRate);
    blockWatcher = false;
  }

  /**
   * does not trigger call to registered ExchangeRateWatcher; calculates inverse rate, and sets both values
   * @param rate
   */
  public void setRate(@NonNull BigDecimal rate) {
    blockWatcher = true;
    rate1Edit.setAmount(rate);
    rate2Edit.setAmount(calculateInverse(rate));
    blockWatcher = false;
  }

  public void setSymbols(String symbol1, String symbol2) {
    setSymbols(rate1Container, symbol1, symbol2);
    setSymbols(rate2Container, symbol2, symbol1);
  }

  private void setSymbols(ViewGroup group, String symbol1, String symbol2) {
    ((TextView) group.findViewById(R.id.ExchangeRateLabel_1)).setText(String.format("1 %s =", symbol1));
    ((TextView) group.findViewById(R.id.ExchangeRateLabel_2)).setText(symbol2);
  }

  private class LinkedExchangeRateTextWatchter implements TextWatcher {
    /**
     * true if we are linked to exchange rate where unit is from account currency
     */
    private boolean isMain;

    LinkedExchangeRateTextWatchter(boolean isMain) {
      this.isMain = isMain;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
      if (blockWatcher) return;
      blockWatcher = true;
      BigDecimal inputRate = getRate(!isMain);
      if (inputRate == null) inputRate = nullValue;
      BigDecimal inverseInputRate = calculateInverse(inputRate);
      (isMain ? rate2Edit : rate1Edit).setAmount(inverseInputRate);
      if (exchangeRateWatcher != null) {
        if (isMain) {
          exchangeRateWatcher.afterExchangeRateChanged(inputRate, inverseInputRate);
        } else {
          exchangeRateWatcher.afterExchangeRateChanged(inverseInputRate, inputRate);
        }
      }
      blockWatcher = false;
    }
  }

  public BigDecimal getRate(boolean inverse) {
    return (inverse ? rate2Edit : rate1Edit).validate(false);
  }

  private BigDecimal calculateInverse(BigDecimal input) {
    return input.compareTo(nullValue) != 0 ?
        new BigDecimal(1).divide(input, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.DOWN) :
        nullValue;
  }

  @Override
  public void setOnFocusChangeListener(OnFocusChangeListener l) {
    rate1Edit.setOnFocusChangeListener(l);
    rate2Edit.setOnFocusChangeListener(l);
  }
}
