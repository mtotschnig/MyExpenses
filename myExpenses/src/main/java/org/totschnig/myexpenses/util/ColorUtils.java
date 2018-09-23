package org.totschnig.myexpenses.util;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class ColorUtils {
  /**
   * from {@link com.github.mikephil.charting.utils.ColorTemplate}
   */
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
}
