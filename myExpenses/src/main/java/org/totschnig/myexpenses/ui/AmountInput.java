package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.totschnig.myexpenses.R;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AmountInput extends LinearLayout {
  @BindView(R.id.TaType)
  protected CompoundButton typeButton;
  @BindView(R.id.AmountEditText)
  protected AmountEditText amountEditText;

  private TypeChangedListener typeChangedListener;

  public void setTypeChangedListener(TypeChangedListener typeChangedListener) {
    this.typeChangedListener = typeChangedListener;
  }

  public void addTextChangedListener(TextWatcher textWatcher) {
    amountEditText.addTextChangedListener(textWatcher);
  }

  public void setType(boolean type) {
    typeButton.setChecked(type);
  }

  public AmountEditText getAmountEditText() {
    return amountEditText;
  }

  public void setFractionDigits(int i) {
    amountEditText.setFractionDigits(i);
  }

  public void setAmount(BigDecimal amount) {
    amountEditText.setAmount(amount.abs());
    typeButton.setChecked(amount.signum() > -1);
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
    return typeButton.isChecked();
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

  public interface TypeChangedListener {
    void onTypeChanged(boolean type);
  }

  public AmountInput(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.amount_input, this, true);
    ButterKnife.bind(this);
    typeButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
      buttonView.setContentDescription(getContext().getString(
          isChecked ? R.string.pm_type_credit : R.string.pm_type_debit));
      if (typeChangedListener != null) {
        typeChangedListener.onTypeChanged(isChecked);
      }
    });
  }

  @Override
  public void setOnFocusChangeListener(OnFocusChangeListener l) {
    amountEditText.setOnFocusChangeListener(l);
    typeButton.setOnFocusChangeListener(l);
  }
}
