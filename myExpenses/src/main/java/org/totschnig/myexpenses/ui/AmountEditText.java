package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import icepick.Icepick;
import icepick.State;

public class AmountEditText extends AppCompatEditText {

  @State
  int fractionDigits = -1;

  DecimalFormat numberFormat = new DecimalFormat();


  public AmountEditText(Context context) {
    super(context);
  }

  public AmountEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AmountEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override public Parcelable onSaveInstanceState() {
    return Icepick.saveInstanceState(this, super.onSaveInstanceState());
  }

  @Override public void onRestoreInstanceState(Parcelable state) {
    super.onRestoreInstanceState(Icepick.restoreInstanceState(this, state));
    setFractionDigitsInternal(fractionDigits);
  }

  public DecimalFormat getNumberFormat() {
    return numberFormat;
  }

  public int getFractionDigits() {
    return fractionDigits;
  }
  public void setFractionDigits(int fractionDigits) {
    if (this.fractionDigits != fractionDigits) {
      setFractionDigitsInternal(fractionDigits);
    }
  }

  private void setFractionDigitsInternal(int fractionDigits) {
    char decimalSeparator = Utils.getDefaultDecimalSeparator();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    String pattern = "#0";
    if (fractionDigits > 0) {
      pattern += "." + new String(new char[fractionDigits]).replace("\0", "#");
    }
    numberFormat = new DecimalFormat(pattern, symbols);
    numberFormat.setGroupingUsed(false);
    configDecimalSeparator(decimalSeparator, fractionDigits);
    //if the new configuration has less fraction digits, we might have to truncate the input
    if (this.fractionDigits != -1 && this.fractionDigits > fractionDigits) {
      String currentText = getText().toString();
      int decimalSeparatorIndex = currentText.indexOf(decimalSeparator);
      if (decimalSeparatorIndex != -1) {
        String minorPart = currentText.substring(decimalSeparatorIndex + 1);
        if (minorPart.length() > fractionDigits) {
          String newText = currentText.substring(0, decimalSeparatorIndex);
          if (fractionDigits > 0) {
            newText += decimalSeparator + minorPart.substring(0, fractionDigits);
          }
          setText(newText);
        }
      }
    }
    this.fractionDigits = fractionDigits;
  }

  public void setAmount(@NonNull BigDecimal amount) {
    setText(numberFormat.format(amount));
  }

  @Nullable
  public BigDecimal validate(boolean showToUser) {
    String strAmount = getText().toString();
    if (strAmount.equals("")) {
      if (showToUser)
        setError(getContext().getString(R.string.required));
      return null;
    }
    BigDecimal amount = Utils.validateNumber(getNumberFormat(), strAmount);
    if (amount == null) {
      if (showToUser)
        setError(getContext().getString(R.string.invalid_number_format, getNumberFormat().format
            (11.11)));
      return null;
    }
    return amount;
  }

  private void configDecimalSeparator(final char decimalSeparator, final int fractionDigits) {
    // TODO we should take into account the arab separator as well
    final char otherSeparator = decimalSeparator == '.' ? ',' : '.';
    setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    setKeyListener(DigitsKeyListener.getInstance(getContext().getString(R.string.amount_digits)));
    setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
      int separatorPositionInDest = dest.toString().indexOf(decimalSeparator);
      char[] v = new char[end - start];
      android.text.TextUtils.getChars(source, start, end, v, 0);
      String input = new String(v).replace(otherSeparator, decimalSeparator);
      if (fractionDigits == 0 || separatorPositionInDest != -1 || dest.length() - dend > fractionDigits) {
        input = input.replace(String.valueOf(decimalSeparator), "");
      } else {
        int separatorPositionInSource = input.lastIndexOf(decimalSeparator);
        if (separatorPositionInSource != -1) {
          //we make sure there is only one separator in the input and after the separator we do not use
          //more minor digits as allowed
          int existingMinorUnits = dest.length() - dend;
          int additionalAllowedMinorUnits = fractionDigits - existingMinorUnits;
          int additionalPossibleMinorUnits = input.length() - separatorPositionInSource - 1;
          int extractMinorUnits = Math.min(additionalPossibleMinorUnits, additionalAllowedMinorUnits);
          input = input.substring(0, separatorPositionInSource).replace(String.valueOf
              (decimalSeparator), "") +
              decimalSeparator + (extractMinorUnits > 0 ?
              input.substring(separatorPositionInSource + 1,
                  separatorPositionInSource + 1 + extractMinorUnits) :
              "");
        }
      }
      if (fractionDigits == 0) {
        return input;
      }
      if (separatorPositionInDest != -1 &&
          dend > separatorPositionInDest && dstart > separatorPositionInDest) {
        int existingMinorUnits = dest.length() - (separatorPositionInDest + 1);
        int remainingMinorUnits = fractionDigits - existingMinorUnits;
        if (remainingMinorUnits < 1) {
          return "";
        }
        return input.length() > remainingMinorUnits ? input.substring(0, remainingMinorUnits) :
            input;
      } else {
        return input;
      }
    }, new InputFilter.LengthFilter(16)});
  }
}
