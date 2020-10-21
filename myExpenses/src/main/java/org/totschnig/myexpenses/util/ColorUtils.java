package org.totschnig.myexpenses.util;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseIntArray;

import com.annimon.stream.Collectors;
import com.annimon.stream.IntStream;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.graphics.ColorUtils.blendARGB;
import static androidx.core.graphics.ColorUtils.calculateLuminance;

public class ColorUtils {
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
  /**
   * from {@link com.github.mikephil.charting.utils.ColorTemplate}
   */
  private static String MAIN_COLORS_AS_TABLE;
  public static final int[] MAIN_COLORS = {
      //PASTEL
      Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), Color.rgb(217, 184, 162),
      Color.rgb(191, 134, 134), Color.rgb(179, 48, 80),
      //JOYFUL
      Color.rgb(217, 80, 138), Color.rgb(254, 149, 7), Color.rgb(254, 247, 120),
      Color.rgb(106, 167, 134), Color.rgb(53, 194, 209),
      //LIBERTY
      Color.rgb(207, 248, 246), Color.rgb(148, 212, 212), Color.rgb(136, 180, 187),
      Color.rgb(118, 174, 175), Color.rgb(42, 109, 130),
      //VORDIPLOM
      Color.rgb(192, 255, 140), Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
      Color.rgb(140, 234, 255), Color.rgb(255, 140, 157),
      //COLORFUL
      Color.rgb(193, 37, 82), Color.rgb(255, 102, 0), Color.rgb(245, 199, 0),
      Color.rgb(106, 150, 31), Color.rgb(179, 100, 53),
      ColorTemplate.getHoloBlue()
  };


  public static String MAIN_COLORS_AS_TABLE() {
    if (MAIN_COLORS_AS_TABLE == null) {
      MAIN_COLORS_AS_TABLE = "(" + IntStream.range(0, MAIN_COLORS.length)
          .mapToObj(i -> "SELECT " + MAIN_COLORS[i] + (i == 0 ? " AS color" : ""))
          .collect(Collectors.joining(" UNION ")) + ") as t";
    }
    return MAIN_COLORS_AS_TABLE;
  }

  /**
   * inspired by http://highintegritydesign.com/tools/tinter-shader/scripts/shader-tinter.js
   */
  public static List<Integer> getShades(int color) {
    ArrayList<Integer> result = new ArrayList<>();
    int red = Color.red(color);
    int redDecrement = (int) Math.round(red * 0.1);
    int green = Color.green(color);
    int greenDecrement = (int) Math.round(green * 0.1);
    int blue = Color.blue(color);
    int blueDecrement = (int) Math.round(blue * 0.1);
    for (int i = 0; i < 10; i++) {
      red = red - redDecrement;
      if (red <= 0) {
        red = 0;
      }
      green = green - greenDecrement;
      if (green <= 0) {
        green = 0;
      }
      blue = blue - blueDecrement;
      if (blue <= 0) {
        blue = 0;
      }
      result.add(Color.rgb(red, green, blue));
    }
    result.add(Color.BLACK);
    return result;
  }

  /**
   * inspired by http://highintegritydesign.com/tools/tinter-shader/scripts/shader-tinter.js
   */
  public static List<Integer> getTints(int color) {
    ArrayList<Integer> result = new ArrayList<>();
    int red = Color.red(color);
    int redIncrement = (int) Math.round((255 - red) * 0.1);
    int green = Color.green(color);
    int greenIncrement = (int) Math.round((255 - green) * 0.1);
    int blue = Color.blue(color);
    int blueIncrement = (int) Math.round((255 - blue) * 0.1);
    for (int i = 0; i < 10; i++) {
      red = red + redIncrement;
      if (red >= 255) {
        red = 255;
      }
      green = green + greenIncrement;
      if (green >= 255) {
        red = 255;
      }
      blue = blue + blueIncrement;
      if (blue >= 255) {
        red = 255;
      }
      result.add(Color.rgb(red, green, blue));
    }
    result.add(Color.WHITE);
    return result;
  }


  public static Drawable createBackgroundColorDrawable(int color) {
    GradientDrawable mask = new GradientDrawable();
    mask.setShape(GradientDrawable.OVAL);
    mask.setColor(color);
    return mask;
  }

  public static int get700Tint(int color) {
    int found = colorPrimaryDarkMap.get(color);
    return found != 0 ? found : color;
  }

  public static boolean isBrightColor(int color) {
    if (android.R.color.transparent == color)
      return true;

    return calculateLuminance(color) > 0.5;
  }

  public static int getComplementColor(int colorInt) {
    final int contrastColor =  MoreUiUtilsKt.getBestForeground(colorInt);
    return blendARGB(colorInt, contrastColor, 0.5F);
  }
}
