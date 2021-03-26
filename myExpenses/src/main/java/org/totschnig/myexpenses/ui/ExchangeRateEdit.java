package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.google.android.material.snackbar.Snackbar;

import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.databinding.ExchangeRateBinding;
import org.totschnig.myexpenses.databinding.ExchangeRatesBinding;
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

public class ExchangeRateEdit extends ConstraintLayout {

  public interface ExchangeRateWatcher {
    void afterExchangeRateChanged(BigDecimal rate, BigDecimal inverse);
  }

  private static final int EXCHANGE_RATE_FRACTION_DIGITS = 5;
  private static final BigDecimal nullValue = new BigDecimal(0);

  AmountEditText rate1Edit;
  AmountEditText rate2Edit;


  private ExchangeRateWatcher exchangeRateWatcher;
  private boolean blockWatcher = false;
  private ExchangeRateViewModel viewModel;
  private CurrencyUnit firstCurrency, secondCurrency;

  private final ExchangeRatesBinding binding = ExchangeRatesBinding.inflate(LayoutInflater.from(getContext()), this);

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

  public void setBlockWatcher(boolean blockWatcher) {
    this.blockWatcher = blockWatcher;
  }

  public ExchangeRateEdit(Context context, AttributeSet attrs) {
    super(context, attrs);
    binding.ivDownload.getRoot().setOnClickListener(v -> {
      if (firstCurrency != null && secondCurrency != null && viewModel != null) {
        viewModel.loadExchangeRate(firstCurrency.getCode(), secondCurrency.getCode(), getHost().getDate());
      }
    });
    rate1Edit = binding.ExchangeRate1.ExchangeRateText;
    rate1Edit.setId(R.id.ExchangeRateEdit1);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      binding.ExchangeRate1.ExchangeRateLabel1.setLabelFor(R.id.ExchangeRateEdit1);
    }
    rate2Edit =  binding.ExchangeRate2.ExchangeRateText;
    rate2Edit.setId(R.id.ExchangeRateEdit2);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      binding.ExchangeRate2.ExchangeRateLabel1.setLabelFor(R.id.ExchangeRateEdit2);
    }
    rate1Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate2Edit.setFractionDigits(EXCHANGE_RATE_FRACTION_DIGITS);
    rate1Edit.addTextChangedListener(new LinkedExchangeRateTextWatcher(true));
    rate2Edit.addTextChangedListener(new LinkedExchangeRateTextWatcher(false));
  }

  /**
   * does not trigger call to registered ExchangeRateWatcher calculates rates based on two values
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
      setSymbols(binding.ExchangeRate1, firstCurrency.getSymbol(), secondCurrency.getSymbol());
      setSymbols(binding.ExchangeRate2, secondCurrency.getSymbol(), firstCurrency.getSymbol());
    }
  }

  private void setSymbols(ExchangeRateBinding group, String symbol1, String symbol2) {
    group.ExchangeRateLabel1.setText(String.format("1 %s =", symbol1));
    group.ExchangeRateLabel2.setText(symbol2);
  }

  private class LinkedExchangeRateTextWatcher implements TextWatcher {
    /**
     * true if we are linked to exchange rate where unit is from account currency
     */
    private final boolean isMain;

    LinkedExchangeRateTextWatcher(boolean isMain) {
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
    ((BaseActivity) host).showSnackbar(message, Snackbar.LENGTH_LONG);
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
    @NonNull
    LocalDate getDate();
  }
}
