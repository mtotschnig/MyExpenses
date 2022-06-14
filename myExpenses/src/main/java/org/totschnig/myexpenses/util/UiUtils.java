package org.totschnig.myexpenses.util;

import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_TIME;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSACTION_WITH_VALUE_DATE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.preference.PrefHandler;

public class UiUtils {

  private UiUtils() {
  }

  public static void increaseSnackbarMaxLines(Snackbar snackbar) {
    View snackbarView = snackbar.getView();
    TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
    textView.setMaxLines(10);
  }

  public static Bitmap getTintedBitmapForTheme(Context context, int drawableResId, int themeResId) {
    Drawable d = getTintedDrawableForContext(new ContextThemeWrapper(context, themeResId), drawableResId);
    return drawableToBitmap(d);
  }

  static Drawable getTintedDrawableForContext(Context context, int drawableResId) {
    return AppCompatResources.getDrawable(context, drawableResId);
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
    ImageViewCompat.setImageTintList(fab, ColorStateList.valueOf(MoreUiUtilsKt.getBestForeground(color)));
  }

  public static void setBackgroundOnButton(AppCompatButton button, int color) {
    //noinspection RestrictedApi
    button.setSupportBackgroundTintList(new ColorStateList(new int[][]{{0}}, new int[]{color}));
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
    DATE, DATE_TIME, BOOKING_VALUE
  }

  public static @NonNull DateMode getDateMode(AccountType accountType, PrefHandler prefHandler) {
    if (!(accountType == AccountType.CASH)) {
      if (prefHandler.getBoolean(TRANSACTION_WITH_VALUE_DATE, false)) {
        return DateMode.BOOKING_VALUE;
      }
    }
    return prefHandler.getBoolean(TRANSACTION_WITH_TIME, true) ?
        DateMode.DATE_TIME : DateMode.DATE;
  }

  public static void configureProgress(DonutProgress donutProgress, int progress) {
    donutProgress.setProgress(Math.min(progress, 100));
    donutProgress.setText(progress < 1000 ? String.valueOf(progress) : ">1k");
  }

  public static int dp2Px(float dp, Resources resources) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
  }

  public static int sp2Px(float sp, Resources resources) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
  }

  public static int px2Dp(int px) {
    return (int) (px / Resources.getSystem().getDisplayMetrics().density);
  }

  public static int resolveIcon(Context context, String resourceName) {
    return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
  }

  /**
   * Returns the value of the desired theme integer attribute
   * @throws android.content.res.Resources.NotFoundException if the given ID
   *         does not exist.
   **/
  @ColorInt
  public static int getColor(Context context, @AttrRes int attr) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attr, typedValue, true);
    return ContextCompat.getColor(context, typedValue.resourceId);
  }

  public static boolean themeBoolAttr(Context context, int attr) {
    TypedArray themeArray = context.getTheme().obtainStyledAttributes(new int[]{attr});
    boolean result = themeArray.getBoolean(0, false);
    themeArray.recycle();
    return result;
  }
}
