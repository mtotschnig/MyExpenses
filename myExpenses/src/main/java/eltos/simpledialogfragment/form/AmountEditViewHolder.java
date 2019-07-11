package eltos.simpledialogfragment.form;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.google.android.material.textfield.TextInputLayout;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.ui.AmountEditText;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;

class AmountEditViewHolder extends FormElementViewHolder<AmountEdit> {
  @BindView(R.id.inputLayout)
  TextInputLayout inputLayout;
  @BindView(R.id.amount)
  AmountEditText amountEditText;

  protected AmountEditViewHolder(AmountEdit field) {
    super(field);
  }

  @Override
  protected int getContentViewLayout() {
    return R.layout.simpledialogfragment_form_item_amount;
  }

  @Override
  protected void setUpView(View view, Context context, Bundle savedInstanceState, SimpleFormDialog.DialogActions actions) {
    ButterKnife.bind(this, view);
    inputLayout.setHint(field.getText(context));
    amountEditText.setFractionDigits(field.fractionDigits);
    if (field.amount != null) {
      amountEditText.setAmount(field.amount);
    }
    // Positive button state for single element forms
    if (actions.isOnlyFocusableElement()) {
      amountEditText.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
          actions.updatePosButtonState();
        }
      });
    }
  }

  @Override
  protected void saveState(Bundle outState) {

  }

  @Override
  protected void putResults(Bundle results, String key) {
    results.putSerializable(key, amountEditText.validate(false));
  }

  @Override
  protected boolean focus(SimpleFormDialog.FocusActions actions) {
    return amountEditText.requestFocus();
  }

  @Override
  protected boolean posButtonEnabled(Context context) {
    if (!field.required) return true;
    final Editable text = amountEditText.getText();
    return text != null && !TextUtils.isEmpty(text.toString());
  }

  @Override
  protected boolean validate(Context context) {
    final BigDecimal result = amountEditText.validate(true);
    if (result == null) return false;
    if (field.max != null && result.compareTo(field.max) > 0) {
      inputLayout.setError(field.maxExceededError);
      return false;
    }
    if (field.min != null && result.compareTo(field.min) < 0) {
      inputLayout.setError(field.underMinError);
      return false;
    }
    return true;
  }
}
