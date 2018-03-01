package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class AmountEditText extends AppCompatEditText {

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


  public DecimalFormat getNumberFormat() {
    return numberFormat;
  }

  public int getFractionDigits() {
    return fractionDigits;
  }

  public void setFractionDigits(int fractionDigits) {
    if (this.fractionDigits == fractionDigits) return;
    char decimalSeparator = Utils.getDefaultDecimalSeparator();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    String pattern = "#0";
    if (fractionDigits > 0) {
      pattern += "." + new String(new char[fractionDigits]).replace("\0", "#");
    }
    numberFormat = new DecimalFormat(pattern,symbols);
    numberFormat.setGroupingUsed(false);
    UiUtils.configDecimalSeparator(this, decimalSeparator,fractionDigits);
    //if the new configuration has less fraction digits, we might have to truncate the input
    if (this.fractionDigits != -1 && this.fractionDigits > fractionDigits) {
      String currentText = getText().toString();
      int decimalSeparatorIndex = currentText.indexOf(decimalSeparator);
      if (decimalSeparatorIndex != -1) {
        String minorPart = currentText.substring(decimalSeparatorIndex + 1);
        if (minorPart.length() > fractionDigits) {
          String newText = currentText.substring(0, decimalSeparatorIndex);
          if (fractionDigits > 0) {
            newText += String.valueOf(decimalSeparator) + minorPart.substring(0, fractionDigits);
          }
          setText(newText);
        }
      }
    }
    this.fractionDigits = fractionDigits;
  }

  public void setAmount(BigDecimal amount) {
    setText(numberFormat.format(amount));
  }

  public BigDecimal validate(boolean showToUser) {
    String strAmount = getText().toString();
    if (strAmount.equals("")) {
      if (showToUser)
        setError(getContext().getString(R.string.no_amount_given));
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
}
