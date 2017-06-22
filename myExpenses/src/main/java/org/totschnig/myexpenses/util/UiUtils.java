package org.totschnig.myexpenses.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.*;
import android.util.SparseIntArray;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;

public class UiUtils {
  /*
  from https://www.google.com/design/spec/style/color.html#color-color-palette
  maps the 500 color to the 700 color
   */
  static final SparseIntArray colorPrimaryDarkMap = new SparseIntArray() {
    {
      append(0xffF44336, 0xffD32F2F);
      append(0xffE91E63, 0xffC2185B);
      append(0xff9C27B0, 0xff7B1FA2);
      append(0xff673AB7, 0xff512DA8);
      append(0xff3F51B5, 0xff303F9F);
      append(0xff2196F3, 0xff1976D2);
      append(0xff03A9F4, 0xff0288D1);
      append(0xff00BCD4, 0xff0097A7);
      append(0xff009688, 0xff00796B);
      append(0xff4CAF50, 0xff388E3C);
      append(0xff8BC34A, 0xff689F38);
      append(0xffCDDC39, 0xffAFB42B);
      append(0xffFFEB3B, 0xffFBC02D);
      append(0xffFFC107, 0xffFFA000);
      append(0xffFF9800, 0xffF57C00);
      append(0xffFF5722, 0xffE64A19);
      append(0xff795548, 0xff5D4037);
      append(0xff9E9E9E, 0xff616161);
      append(0xff607D8B, 0xff455A64);
      append(0xff757575, 0xff424242); //aggregate theme light 600 800
      append(0xffBDBDBD, 0xff757575); //aggregate theme dark  400 600
    }
  };

  private UiUtils() {}

  public static void configureSnackbarForDarkTheme(Snackbar snackbar) {
    if (MyApplication.getThemeType().equals(MyApplication.ThemeType.dark)) {
      //Workaround for https://issuetracker.google.com/issues/37120757
      View snackbarView = snackbar.getView();
      snackbarView.setBackgroundColor(Color.WHITE);
      TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
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

  public static int get700Tint(int color) {
    int found = colorPrimaryDarkMap.get(color);
    return found != 0 ? found : color;
  }

  public static Bitmap getTintedBitmapForTheme(Context context, int drawableResId, int themeResId) {
    Drawable d = getTintedDrawableForTheme(context, drawableResId, themeResId);
    return drawableToBitmap(d);
  }

  static Drawable getTintedDrawableForTheme(Context context, int drawableResId, int themeResId) {
    Context wrappedContext = new ContextThemeWrapper(context, themeResId);
    //noinspection RestrictedApi
    return AppCompatDrawableManager.get().getDrawable(wrappedContext, drawableResId);
  }

  static Bitmap drawableToBitmap(Drawable d) {
    Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(),
        d.getIntrinsicHeight(),
        Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    d.setBounds(0, 0, c.getWidth(), c.getHeight());
    d.draw(c);
    return b;
  }

  public static boolean isBrightColor(int color) {
    if (android.R.color.transparent == color)
      return true;

    boolean rtnValue = false;

    int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

    int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
        * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

    // color is light
    if (brightness >= 200) {
      rtnValue = true;
    }

    return rtnValue;
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

  @SuppressLint("NewApi")
  public static void setBackgroundTintListOnFab(FloatingActionButton fab, int color) {
    fab.setBackgroundTintList(ColorStateList.valueOf(color));
    DrawableCompat.setTint(fab.getDrawable(), isBrightColor(color) ? Color.BLACK : Color.WHITE);
    fab.invalidate();
  }
}
