package org.totschnig.myexpenses.util.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;

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

  public static Drawable getTintedDrawableForContext(Context context, int drawableResId) {
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

  public static void configureAmountTextViewForHebrew(TextView amount) {
    int layoutDirection = amount.getContext().getResources().getInteger(R.integer.amount_layout_direction);
    if (layoutDirection == 0) { // hebrew
      amount.setLayoutDirection(layoutDirection);
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

  public static int dp2Px(float dp, Resources resources) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
  }

  public static int sp2Px(float sp, Resources resources) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
  }

  public static int px2Dp(int px) {
    return (int) (px / Resources.getSystem().getDisplayMetrics().density);
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
}
