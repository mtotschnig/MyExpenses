package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.preference.PrefHandler;

import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_TIME;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_VALUE_DATE;

public class UiUtils {

  private UiUtils() {}

  public static void configureSnackbarForDarkTheme(Snackbar snackbar) {
    if (MyApplication.getThemeType().equals(MyApplication.ThemeType.dark)) {
      //Workaround for https://issuetracker.google.com/issues/37120757
      View snackbarView = snackbar.getView();
      snackbarView.setBackgroundColor(Color.WHITE);
      TextView textView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
      textView.setTextColor(Color.BLACK);
    }
  }

  public static void configDecimalSeparator(final EditText editText,
                                            final char decimalSeparator, final int fractionDigits) {
    // mAmountText.setInputType(
    // InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
    // due to bug in Android platform
    // http://code.google.com/p/android/issues/detail?id=2626
    // the soft keyboard if it occupies full screen in horizontal orientation
    // does not display the , as comma separator
    // TODO we should take into account the arab separator as well
    final char otherSeparator = decimalSeparator == '.' ? ',' : '.';
    editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    editText.setFilters(new InputFilter[]{new InputFilter() {
      @Override
      public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                                 int dend) {
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
            int extractMinorUnits = additionalPossibleMinorUnits >= additionalAllowedMinorUnits ?
                additionalAllowedMinorUnits : additionalPossibleMinorUnits;
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
      }
    }, new InputFilter.LengthFilter(16)});
  }

  public static Bitmap getTintedBitmapForTheme(Context context, int drawableResId, int themeResId) {
    Drawable d = getTintedDrawableForContext(new ContextThemeWrapper(context, themeResId), drawableResId);
    return drawableToBitmap(d);
  }

  static Drawable getTintedDrawableForContext(Context context, int drawableResId) {
    //noinspection RestrictedApi
    return AppCompatDrawableManager.get().getDrawable(context, drawableResId);
  }

  public static Bitmap drawableToBitmap(Drawable d) {
    Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(),
        d.getIntrinsicHeight(),
        Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    d.setBounds(0, 0, c.getWidth(), c.getHeight());
    d.draw(c);
    return b;
  }

  //http://stackoverflow.com/a/11072627/1199911
  public static void selectSpinnerItemByValue(Spinner spnr, long value) {
    SimpleCursorAdapter adapter = (SimpleCursorAdapter) spnr.getAdapter();
    for (int position = 0; position < adapter.getCount(); position++) {
      if (adapter.getItemId(position) == value) {
        spnr.setSelection(position);
        return;
      }
    }
  }

  public static void setBackgroundTintListOnFab(FloatingActionButton fab, int color) {
    fab.setBackgroundTintList(ColorStateList.valueOf(color));
    DrawableCompat.setTint(fab.getDrawable(), ColorUtils.isBrightColor(color) ? Color.BLACK : Color.WHITE);
    fab.invalidate();
  }

  public static void setBackgroundOnButton(AppCompatButton button, int color) {
    //noinspection RestrictedApi
    button.setSupportBackgroundTintList(new ColorStateList(new int[][] {{0}}, new int[] {color}));
  }

  public static void configureAmountTextViewForHebrew(TextView amount) {
    int layoutDirection = amount.getContext().getResources().getInteger(R.integer.amount_layout_direction);
    if (layoutDirection == 0) { // hebrew
      ViewCompat.setLayoutDirection(amount, layoutDirection);
      amount.setEms(5);
      amount.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
      amount.setSingleLine(true);
      amount.setMarqueeRepeatLimit(-1);
      amount.setHorizontallyScrolling(true);
      amount.setSelected(true);
    }
  }

  public enum DateMode {
    DATE, DATE_TIME, BOOKING_VALUE;
  }

  public static DateMode getDateMode(Account account, PrefHandler prefHandler) {
    if (!(account.getType() == AccountType.CASH)) {
      if (prefHandler.getBoolean(TRANSACTION_WITH_VALUE_DATE, false)) {
        return DateMode.BOOKING_VALUE;
      }
    }
    return prefHandler.getBoolean(TRANSACTION_WITH_TIME, true) ?
        DateMode.DATE_TIME : DateMode.DATE;
  }
}
