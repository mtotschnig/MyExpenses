package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.databinding.AmountInputAlternateBinding;
import org.totschnig.myexpenses.databinding.AmountInputBinding;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.math.BigDecimal;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.viewbinding.ViewBinding;

public class AmountInput extends ConstraintLayout {
  public CompoundButton typeButton() {
    return (viewBinding instanceof AmountInputAlternateBinding ?
        ((AmountInputAlternateBinding) viewBinding).TaType :
        ((AmountInputBinding) viewBinding).TaType)
        .getRoot();
  }

  private AmountEditText amountEditText() {
    return (viewBinding instanceof AmountInputAlternateBinding ?
        ((AmountInputAlternateBinding) viewBinding).AmountEditText :
        ((AmountInputBinding) viewBinding).AmountEditText)
        .getRoot();
  }

  private ImageView calculator() {
    return (viewBinding instanceof AmountInputAlternateBinding ?
        ((AmountInputAlternateBinding) viewBinding).Calculator :
        ((AmountInputBinding) viewBinding).Calculator)
        .getRoot();
  }

  private AppCompatSpinner currencySpinner() {
    return (viewBinding instanceof AmountInputAlternateBinding ?
        ((AmountInputAlternateBinding) viewBinding).AmountCurrency :
        ((AmountInputBinding) viewBinding).AmountCurrency)
        .getRoot();
  }

  private ExchangeRateEdit exchangeRateEdit() {
    return (viewBinding instanceof AmountInputAlternateBinding ?
        ((AmountInputAlternateBinding) viewBinding).AmountExchangeRate :
        ((AmountInputBinding) viewBinding).AmountExchangeRate)
        .getRoot();
  }


  private ViewBinding viewBinding;
  private boolean withTypeSwitch;
  private boolean withCurrencySelection;
  private boolean withExchangeRate;
  private TypeChangedListener typeChangedListener;
  private CompoundResultOutListener compoundResultOutListener;
  private BigDecimal compoundResultInput;
  private boolean initialized;

  private CurrencyAdapter currencyAdapter;
  private CurrencyContext currencyContext;

  public AmountInput(Context context) {
    super(context);
    init(null);
  }

  public AmountInput(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public AmountInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    final Context context = getContext();
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AmountInput);
    withTypeSwitch = ta.getBoolean(R.styleable.AmountInput_withTypeSwitch, true);
    withCurrencySelection = ta.getBoolean(R.styleable.AmountInput_withCurrencySelection, false);
    withExchangeRate = ta.getBoolean(R.styleable.AmountInput_withExchangeRate, false);
    boolean alternateLayout = ta.getBoolean(R.styleable.AmountInput_alternateLayout, false);
    ta.recycle();
    LayoutInflater inflater = LayoutInflater.from(context);
    viewBinding = alternateLayout ? AmountInputAlternateBinding.inflate(inflater, this) : AmountInputBinding.inflate(inflater, this);
    updateChildContentDescriptions();
    if (withTypeSwitch) {
      typeButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
        setContentDescriptionForTypeSwitch();
        if (typeChangedListener != null) {
          typeChangedListener.onTypeChanged(isChecked);
        }
      });
    } else {
      typeButton().setVisibility(View.GONE);
    }
    if (withCurrencySelection) {
      currencyAdapter = new CurrencyAdapter(getContext(), android.R.layout.simple_spinner_item) {
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
          View view = super.getView(position, convertView, parent);
          view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), 0, view.getPaddingBottom());
          ((TextView) view).setText(getItem(position).getCode());
          return view;
        }
      };
      currencySpinner().setAdapter(currencyAdapter);
      currencySpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          String currency = ((Currency) currencySpinner().getSelectedItem()).getCode();
          final CurrencyUnit currencyUnit = currencyContext.get(currency);
          amountEditText().setFractionDigits(currencyUnit.getFractionDigits());
          exchangeRateEdit().setCurrencies(currencyUnit, null);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
      });
    } else {
      currencySpinner().setVisibility(View.GONE);
    }
    if (withExchangeRate) {
      exchangeRateEdit().setExchangeRateWatcher((rate, inverse) -> {
        onCompoundResultOutput();
        onCompoundResultInput();
      });
    } else {
      exchangeRateEdit().setVisibility(View.GONE);
    }
    calculator().setOnClickListener(v -> getHost().showCalculator(validate(false), getId()));
    initialized = true;
  }

  @Override
  public void setContentDescription(CharSequence contentDescription) {
    super.setContentDescription(contentDescription);
    if (initialized) {
      updateChildContentDescriptions();
    }
  }

  public void setExchangeRate(BigDecimal rate) {
    exchangeRateEdit().setRate(rate, false);
  }

  private void updateChildContentDescriptions() {
    //Edit Text does not use content description once it holds content. It is hence needed to point a textView
    //in the neighborhood of this AmountInput directly to amountEdiText with android:labelFor="@id/AmountEditText"
    //setContentDescriptionForChild(amountEditText, null);
    setContentDescriptionForChild(calculator(), getContext().getString(R.string.content_description_calculator));
    setContentDescriptionForChild(currencySpinner(), getContext().getString(R.string.currency));
    setContentDescriptionForTypeSwitch();
  }

  private void setContentDescriptionForChild(View view, @Nullable CharSequence contentDescription) {
    view.setContentDescription(contentDescription == null ? getContentDescription() :
        String.format("%s : %s", getContentDescription(), contentDescription));
  }

  private void setContentDescriptionForTypeSwitch() {
    setContentDescriptionForChild(typeButton(), getContext().getString(
        typeButton().isChecked() ? R.string.income : R.string.expense));
  }

  public void setTypeChangedListener(TypeChangedListener typeChangedListener) {
    this.typeChangedListener = typeChangedListener;
  }

  public void setCompoundResultOutListener(CompoundResultOutListener compoundResultOutListener) {
    this.compoundResultInput = null;
    this.compoundResultOutListener = compoundResultOutListener;
    amountEditText().addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        onCompoundResultOutput();
      }
    });
  }

  public void setCompoundResultInput(BigDecimal input) {
    this.compoundResultInput = input;
    onCompoundResultInput();
  }

  private void onCompoundResultOutput() {
    if (compoundResultOutListener == null) return;
    BigDecimal input = validate(false);
    BigDecimal rate = exchangeRateEdit().getRate(false);
    if (input != null && rate != null) {
      compoundResultOutListener.onResultChanged(input.multiply(rate));
    }
  }

  private void onCompoundResultInput() {
    if (compoundResultInput != null) {
      final BigDecimal rate = exchangeRateEdit().getRate(false);
      if (rate != null) {
        setAmount(compoundResultInput.multiply(rate), false);
      }
    }
  }

  public void addTextChangedListener(TextWatcher textWatcher) {
    amountEditText().addTextChangedListener(textWatcher);
  }

  public void setType(boolean type) {
    typeButton().setChecked(type);
  }

  public void setFractionDigits(int i) {
    amountEditText().setFractionDigits(i);
  }

  public void setAmount(@NonNull BigDecimal amount) {
    setAmount(amount, true);
  }

  public void setAmount(@NonNull BigDecimal amount, boolean updateType) {
    amountEditText().setError(null);
    amountEditText().setAmount(amount.abs());
    if (updateType) {
      typeButton().setChecked(amount.signum() > -1);
    }
  }

  public void setRaw(String text) {
    amountEditText().setText(text);
  }

  public void clear() {
    amountEditText().setText("");
  }

  @Nullable
  public BigDecimal validate(boolean showToUser) {
    return amountEditText().validate(showToUser);
  }

  @NonNull
  public BigDecimal getTypedValue() {
    return getTypedValue(false, false);
  }

  public BigDecimal getTypedValue(boolean ifPresent, boolean showToUser) {
    final BigDecimal bigDecimal = validate(showToUser);
    if (bigDecimal == null) {
      return ifPresent ? null : BigDecimal.ZERO;
    } else if (!getType()) {
      return bigDecimal.negate();
    }
    return bigDecimal;
  }

  public boolean getType() {
    return !withTypeSwitch || typeButton().isChecked();
  }

  public void hideTypeButton() {
    typeButton().setVisibility(GONE);
  }

  public void setTypeEnabled(boolean enabled) {
    typeButton().setEnabled(enabled);
  }

  public void toggle() {
    typeButton().toggle();
  }

  public void setCurrencies(List<Currency> currencies, CurrencyContext currencyContext) {
    currencyAdapter.addAll(currencies);
    this.currencyContext = currencyContext;
  }

  public void setSelectedCurrency(String originalCurrencyCode) {
    currencySpinner().setSelection(currencyAdapter.getPosition(Currency.Companion.create(originalCurrencyCode, getContext())));
  }

  public void configureExchange(CurrencyUnit currencyUnit, CurrencyUnit homeCurrency) {
    if (withExchangeRate) {
      exchangeRateEdit().setCurrencies(currencyUnit, homeCurrency);
    }
  }

  /**
   * sets the second currency on the exchangeEdit, the first one taken from the currency selector
   */
  public void configureExchange(CurrencyUnit currencyUnit) {
    if (withExchangeRate && withCurrencySelection) {
      final Currency selectedCurrency = getSelectedCurrency();
      exchangeRateEdit().setCurrencies(selectedCurrency != null ?
          currencyContext.get(selectedCurrency.getCode()) : null, currencyUnit);
    }
  }

  @Nullable
  public Currency getSelectedCurrency() {
    return (Currency) currencySpinner().getSelectedItem();
  }

  public void selectAll() {
    amountEditText().selectAll();
  }

  public void setError(CharSequence error) {
    amountEditText().setError(error);
  }

  /**
   * this amount input is supposed to output the application of the exchange rate to its amount
   * used for the original amount in {@link org.totschnig.myexpenses.activity.ExpenseEdit}
   */
  public interface CompoundResultOutListener {
    void onResultChanged(BigDecimal result);
  }

  public interface TypeChangedListener {
    void onTypeChanged(boolean type);
  }

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
    void showCalculator(BigDecimal amount, int id);

    void setFocusAfterRestoreInstanceState(Pair<Integer, Integer> focusView);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    final View focusedChild = getFocusedChild();
    return new SavedState(superState, typeButton().onSaveInstanceState(),
        amountEditText().onSaveInstanceState(), currencySpinner().onSaveInstanceState(),
        exchangeRateEdit().getRate(false), focusedChild != null ? focusedChild.getId() : 0);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    typeButton().onRestoreInstanceState(savedState.getTypeButtonState());
    amountEditText().onRestoreInstanceState(savedState.getAmountEditTextState());
    currencySpinner().onRestoreInstanceState(savedState.getCurrencySpinnerState());
    exchangeRateEdit().setRate(savedState.getExchangeRateState(), true);
    if (savedState.getFocusedId() != 0) {
      getHost().setFocusAfterRestoreInstanceState(Pair.create(getId(), savedState.getFocusedId()));
    }
  }

  @Override
  protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
    super.dispatchFreezeSelfOnly(container);
  }

  @Override
  protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
    super.dispatchThawSelfOnly(container);
  }

  private static class SavedState extends BaseSavedState {
    private final Parcelable typeButtonState;
    private final Parcelable amountEditTextState;
    private final Parcelable currencySpinnerState;
    private final BigDecimal exchangeRateState;
    private int focusedId;

    private SavedState(Parcel in) {
      super(in);
      final ClassLoader classLoader = getClass().getClassLoader();
      this.typeButtonState = in.readParcelable(classLoader);
      this.amountEditTextState = in.readParcelable(classLoader);
      this.currencySpinnerState = in.readParcelable(classLoader);
      this.exchangeRateState = (BigDecimal) in.readSerializable();
      this.focusedId = in.readInt();
    }

    SavedState(Parcelable superState, Parcelable typeButtonState, Parcelable amountEditTextState,
               Parcelable currencySpinnerState, BigDecimal exchangeRateState, int focusedId) {
      super(superState);
      this.typeButtonState = typeButtonState;
      this.amountEditTextState = amountEditTextState;
      this.currencySpinnerState = currencySpinnerState;
      this.exchangeRateState = exchangeRateState;
      this.focusedId = focusedId;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
      super.writeToParcel(destination, flags);
      destination.writeParcelable(typeButtonState, flags);
      destination.writeParcelable(amountEditTextState, flags);
      destination.writeParcelable(currencySpinnerState, flags);
      destination.writeSerializable(exchangeRateState);
      destination.writeInt(focusedId);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };

    Parcelable getTypeButtonState() {
      return typeButtonState;
    }

    Parcelable getAmountEditTextState() {
      return amountEditTextState;
    }

    Parcelable getCurrencySpinnerState() {
      return currencySpinnerState;
    }

    BigDecimal getExchangeRateState() {
      return exchangeRateState;
    }

    int getFocusedId() {
      return focusedId;
    }
  }
}
