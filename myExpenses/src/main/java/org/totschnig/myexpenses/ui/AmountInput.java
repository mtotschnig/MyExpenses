package org.totschnig.myexpenses.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.math.BigDecimal;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AmountInput extends LinearLayout {
  @BindView(R.id.TaType)
  CompoundButton typeButton;
  @BindView(R.id.AmountEditText)
  AmountEditText amountEditText;
  @BindView(R.id.Calculator)
  View calculator;
  @BindView(R.id.AmountCurrency)
  Spinner currencySpinner;

  private boolean withTypeSwitch;
  private boolean withCurrencySelection;
  private TypeChangedListener typeChangedListener;
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

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public AmountInput(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    final Context context = getContext();
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AmountInput);
    withTypeSwitch = ta.getBoolean(R.styleable.AmountInput_withTypeSwitch, true);
    withCurrencySelection = ta.getBoolean(R.styleable.AmountInput_withCurrencySelection, false);
    ta.recycle();
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.amount_input, this, true);
    ButterKnife.bind(this);
    updateChildContentDescriptions();
    if (withTypeSwitch) {
      typeButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
        setContentDescriptionForTypeSwitch();
        if (typeChangedListener != null) {
          typeChangedListener.onTypeChanged(isChecked);
        }
      });
    } else {
      typeButton.setVisibility(View.GONE);
    }
    if (withCurrencySelection) {
      currencyAdapter = new CurrencyAdapter(getContext(), android.R.layout.simple_spinner_item) {
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
          View view = super.getView(position, convertView, parent);
          view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), 0, view.getPaddingBottom());
          ((TextView) view).setText(getItem(position).code());
          return view;
        }
      };
      currencySpinner.setAdapter(currencyAdapter);
      currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          String currency = ((Currency) currencySpinner.getSelectedItem()).code();
          amountEditText.setFractionDigits(currencyContext.get(currency).fractionDigits());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
      });
    } else {
      currencySpinner.setVisibility(View.GONE);
    }
    calculator.setOnClickListener(v -> {
      getHost().showCalculator(validate(false), getId());
    });
    initialized = true;
  }

  @Override
  public void setContentDescription(CharSequence contentDescription) {
    super.setContentDescription(contentDescription);
    if (initialized) {
      updateChildContentDescriptions();
    }
  }

  private void updateChildContentDescriptions() {
    setContentDescriptionForChild(amountEditText, null);
    setContentDescriptionForChild(calculator, getContext().getString(R.string.content_description_calculator));
    setContentDescriptionForChild(currencySpinner, getContext().getString(R.string.currency));
    setContentDescriptionForTypeSwitch();
  }

  private void setContentDescriptionForChild(View view, @Nullable CharSequence contentDescription) {
    view.setContentDescription(contentDescription == null ? getContentDescription() :
        String.format("%s : %s", getContentDescription(), contentDescription));
  }

  private void setContentDescriptionForTypeSwitch() {
    setContentDescriptionForChild(typeButton, getContext().getString(
        typeButton.isChecked() ? R.string.income : R.string.expense));
  }

  public void setTypeChangedListener(TypeChangedListener typeChangedListener) {
    this.typeChangedListener = typeChangedListener;
  }

  public void addTextChangedListener(TextWatcher textWatcher) {
    amountEditText.addTextChangedListener(textWatcher);
  }

  public void setType(boolean type) {
    typeButton.setChecked(type);
  }

  public void setFractionDigits(int i) {
    amountEditText.setFractionDigits(i);
  }

  public void setAmount(BigDecimal amount) {
    setAmount(amount, true);
  }

  public void setAmount(BigDecimal amount, boolean updateType) {
    amountEditText.setError(null);
    amountEditText.setAmount(amount.abs());
    if (updateType) {
      typeButton.setChecked(amount.signum() > -1);
    }
  }

  public void clear() {
    amountEditText.setText("");
  }

  @Nullable
  public BigDecimal validate(boolean showToUser) {
    return amountEditText.validate(showToUser);
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
    return !withTypeSwitch || typeButton.isChecked();
  }

  public void hideTypeButton() {
    typeButton.setVisibility(GONE);
  }

  public void setTypeEnabled(boolean enabled) {
    typeButton.setEnabled(enabled);
  }

  public void toggle() {
    typeButton.toggle();
  }

  public void setCurrencies(List<Currency> currencies, CurrencyContext currencyContext) {
    currencyAdapter.addAll(currencies);
    this.currencyContext = currencyContext;
  }

  public void setSelectedCurrency(String originalCurrencyCode) {
    currencySpinner.setSelection(currencyAdapter.getPosition(Currency.create(originalCurrencyCode)));
  }

  public Currency getSelectedCurrency() {
    return (Currency) currencySpinner.getSelectedItem();
  }

  public void selectAll() {
    amountEditText.selectAll();
  }

  public void setError(CharSequence error) {
    amountEditText.setError(error);
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
  }


  @Override
  public void setOnFocusChangeListener(OnFocusChangeListener l) {
    amountEditText.setOnFocusChangeListener(l);
    typeButton.setOnFocusChangeListener(l);
    currencySpinner.setOnFocusChangeListener(l);
    calculator.setOnFocusChangeListener(l);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    return new SavedState(superState, typeButton.onSaveInstanceState(), amountEditText.onSaveInstanceState(), currencySpinner.onSaveInstanceState());
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    typeButton.onRestoreInstanceState(savedState.getTypeButtonState());
    amountEditText.onRestoreInstanceState(savedState.getAmountEditTextState());
    currencySpinner.onRestoreInstanceState(savedState.getCurrencySpinnerState());
  }

  @Override
  protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
    super.dispatchFreezeSelfOnly(container);
  }

  @Override
  protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
    super.dispatchThawSelfOnly(container);
  }

  static class SavedState extends BaseSavedState {
    private Parcelable typeButtonState;
    private Parcelable amountEditTextState;
    private Parcelable currencySpinnerState;

    private SavedState(Parcel in) {
      super(in);
      final ClassLoader classLoader = getClass().getClassLoader();
      this.typeButtonState = in.readParcelable(classLoader);
      this.amountEditTextState = in.readParcelable(classLoader);
      this.currencySpinnerState = in.readParcelable(classLoader);
    }

    SavedState(Parcelable superState, Parcelable typeButtonState, Parcelable amountEditTextState, Parcelable currencySpinnerState) {
      super(superState);
      this.typeButtonState = typeButtonState;
      this.amountEditTextState = amountEditTextState;
      this.currencySpinnerState = currencySpinnerState;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
      super.writeToParcel(destination, flags);
      destination.writeParcelable(typeButtonState, flags);
      destination.writeParcelable(amountEditTextState, flags);
      destination.writeParcelable(currencySpinnerState, flags);
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
  }
}
