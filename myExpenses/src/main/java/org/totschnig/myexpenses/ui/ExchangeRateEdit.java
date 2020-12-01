package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.retrofit.MissingAppIdException;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.viewmodel.ExchangeRateViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExchangeRateEdit extends ConstraintLayout {

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


  private ExchangeRateWatcher exchangeRateWatcher;
  private boolean blockWatcher = false;
  private ExchangeRateViewModel viewModel;
  private CurrencyUnit firstCurrency, secondCurrency;

  public void setExchangeRateWatcher(ExchangeRateWatcher exchangeRateWatcher) {
    this.exchangeRateWatcher = exchangeRateWatcher;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    setupViewModel();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    viewModel.clear();
  }

  @Nullable
  public LifecycleOwner findLifecycleOwner(Context context) {
    if (context instanceof LifecycleOwner) {
      return ((LifecycleOwner) context);
    }
    if (context instanceof ContextWrapper) {
      return findLifecycleOwner(((ContextWrapper) context).getBaseContext());
    }
    return null;
  }

  public void setupViewModel() {
    Context context = getContext();
    viewModel = new ExchangeRateViewModel(((MyApplication) context.getApplicationContext()));
    final LifecycleOwner lifecycleOwner = findLifecycleOwner(context);
    if (lifecycleOwner != null) {
      viewModel.getData().observe(lifecycleOwner, result -> rate2Edit.setAmount(BigDecimal.valueOf(result)));
    viewModel.getError().observe(lifecycleOwner, exception -> complain(exception instanceof UnsupportedOperationException ? getContext().getString(
        R.string.exchange_rate_not_supported, firstCurrency.getCode(), secondCurrency.getCode()) :
        (exception instanceof MissingAppIdException ? getContext().getString(R.string.pref_openexchangerates_app_id_summary) :
            exception.getMessage())));
    } else {
      CrashHandler.report("No LifecycleOwner found");
    }
  }

  @OnClick(R.id.iv_download)
  void loadRate() {
    if (firstCurrency != null && secondCurrency != null && viewModel != null) {
      viewModel.loadExchangeRate(firstCurrency.getCode(), secondCurrency.getCode(), getHost().getDate());
    }
  }

  public void setBlockWatcher(boolean blockWatcher) {
    this.blockWatcher = blockWatcher;
  }

  public ExchangeRateEdit(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.exchange_rates, this, true);
    ButterKnife.bind(this);
    rate1Edit = rate1Container.findViewById(R.id.ExchangeRateText);
    rate1Edit.setId(R.id.ExchangeRateEdit1);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      rate1Container.findViewById(R.id.ExchangeRateLabel_1).setLabelFor(R.id.ExchangeRateEdit1);
    }
    rate2Edit = rate2Container.findViewById(R.id.ExchangeRateText);
    rate2Edit.setId(R.id.ExchangeRateEdit2);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      rate2Container.findViewById(R.id.ExchangeRateLabel_1).setLabelFor(R.id.ExchangeRateEdit2);
    }
    rate1Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate2Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate1Edit.addTextChangedListener(new LinkedExchangeRateTextWatchter(true));
    rate2Edit.addTextChangedListener(new LinkedExchangeRateTextWatchter(false));
  }

  /**
   * does not trigger call to registered ExchangeRateWatcher calculates rates based on two values
   *
   * @param amount1
   * @param amount2
   */
  public void calculateAndSetRate(@Nullable BigDecimal amount1, @Nullable BigDecimal amount2) {
    blockWatcher = true;
    BigDecimal exchangeRate;
    BigDecimal inverseExchangeRate;
    if (amount1 != null && amount2 != null && amount1.compareTo(nullValue) != 0 && amount2.compareTo(nullValue) != 0) {
      final BigDecimal a2Abs = amount2.abs();
      final BigDecimal a1Abs = amount1.abs();
      exchangeRate = a2Abs.divide(a1Abs, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.HALF_EVEN);
      inverseExchangeRate = a1Abs.divide(a2Abs, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.HALF_EVEN);
    } else {
      exchangeRate = nullValue;
      inverseExchangeRate = nullValue;
    }
    rate1Edit.setAmount(exchangeRate);
    rate2Edit.setAmount(inverseExchangeRate);
    blockWatcher = false;
  }

  /**
   * does not trigger call to registered ExchangeRateWatcher; calculates inverse rate, and sets both values
   *
   * @param rate
   * @param blockWatcher
   */
  public void setRate(@Nullable BigDecimal rate, boolean blockWatcher) {
    if (rate != null) {
      if (blockWatcher) {
        this.blockWatcher = true;
      }
      rate1Edit.setAmount(rate);
      rate2Edit.setAmount(calculateInverse(rate));
      this.blockWatcher = false;
    }
  }

  public void setCurrencies(@Nullable CurrencyUnit first, @Nullable CurrencyUnit second) {
    if (first != null) {
      this.firstCurrency = first;
    }
    if (second != null) {
      this.secondCurrency = second;
    }
    if (firstCurrency != null && secondCurrency != null) {
      setSymbols(rate1Container, firstCurrency.getSymbol(), secondCurrency.getSymbol());
      setSymbols(rate2Container, secondCurrency.getSymbol(), firstCurrency.getSymbol());
    }
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

  @Nullable
  public BigDecimal getRate(boolean inverse) {
    return (inverse ? rate2Edit : rate1Edit).validate(false);
  }

  private BigDecimal calculateInverse(BigDecimal input) {
    return input.compareTo(nullValue) != 0 ?
        new BigDecimal(1).divide(input, EXCHANGE_RATE_FRACTION_DIGITS, RoundingMode.HALF_EVEN) :
        nullValue;
  }

  private void complain(String message) {
    Host host = getHost();
    host.showSnackbar(message, Snackbar.LENGTH_LONG);
  }

  @NonNull
  protected Host getHost() {
    Context context = getContext();
    while (context instanceof android.content.ContextWrapper) {
      if (context instanceof Host) {
        return (Host) context;
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    throw new IllegalStateException("Host context does not implement interface");
  }

  public interface Host {
    void showSnackbar(@NonNull CharSequence message, int lengthLong);

    @NonNull
    LocalDate getDate();
  }
}
