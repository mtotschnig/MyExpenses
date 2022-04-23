package org.totschnig.myexpenses.util

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.SparseIntArray
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.utils.ColorTemplate
import kotlin.math.roundToInt

object ColorUtils {
    /*
  from https://www.google.com/design/spec/style/color.html#color-color-palette
  maps the 500 color to the 700 color
   */
    private val colorPrimaryDarkMap: SparseIntArray = object : SparseIntArray() {
        init {
            append(-0xbbcca, -0x2cd0d1)
            append(-0x16e19d, -0x3de7a5)
            append(-0x63d850, -0x84e05e)
            append(-0x98c549, -0xaed258)
            append(-0xc0ae4b, -0xcfc061)
            append(-0xde690d, -0xe6892e)
            append(-0xfc560c, -0xfd772f)
            append(-0xff432c, -0xff6859)
            append(-0xff6978, -0xff8695)
            append(-0xb350b0, -0xc771c4)
            append(-0x743cb6, -0x9760c8)
            append(-0x3223c7, -0x504bd5)
            append(-0x14c5, -0x43fd3)
            append(-0x3ef9, -0x6000)
            append(-0x6800, -0xa8400)
            append(-0xa8de, -0x19b5e7)
            append(-0x86aab8, -0xa2bfc9)
            append(-0x616162, -0x9e9e9f)
            append(-0x9f8275, -0xbaa59c)
            append(-0x8a8a8b, -0xbdbdbe) //aggregate theme light 600 800
            append(-0x424243, -0x8a8a8b) //aggregate theme dark  400 600
        }
    }
    /**
     * from [com.github.mikephil.charting.utils.ColorTemplate]
     */
    @JvmField
    val MAIN_COLORS = intArrayOf( //PASTEL
        Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), Color.rgb(217, 184, 162),
        Color.rgb(191, 134, 134), Color.rgb(179, 48, 80),  //JOYFUL
        Color.rgb(217, 80, 138), Color.rgb(254, 149, 7), Color.rgb(254, 247, 120),
        Color.rgb(106, 167, 134), Color.rgb(53, 194, 209),  //LIBERTY
        Color.rgb(207, 248, 246), Color.rgb(148, 212, 212), Color.rgb(136, 180, 187),
        Color.rgb(118, 174, 175), Color.rgb(42, 109, 130),  //VORDIPLOM
        Color.rgb(192, 255, 140), Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
        Color.rgb(140, 234, 255), Color.rgb(255, 140, 157),  //COLORFUL
        Color.rgb(193, 37, 82), Color.rgb(255, 102, 0), Color.rgb(245, 199, 0),
        Color.rgb(106, 150, 31), Color.rgb(179, 100, 53),
        ColorTemplate.getHoloBlue()
    )

    @JvmField
    val MAIN_COLORS_AS_TABLE =
        "(${
            MAIN_COLORS.mapIndexed { index, i -> "SELECT $i${if (index == 0) " AS color" else ""}" }
                .joinToString(separator = " UNION ")
        }) as t"

    /**
     * inspired by http://highintegritydesign.com/tools/tinter-shader/scripts/shader-tinter.js
     */
    fun getShades(color: Int): List<Int> {
        val result = ArrayList<Int>()
        var red = Color.red(color)
        val redDecrement = (red * 0.1).roundToInt()
        var green = Color.green(color)
        val greenDecrement = (green * 0.1).roundToInt()
        var blue = Color.blue(color)
        val blueDecrement = (blue * 0.1).roundToInt()
        for (i in 0..9) {
            red -= redDecrement
            if (red <= 0) {
                red = 0
            }
            green -= greenDecrement
            if (green <= 0) {
                green = 0
            }
            blue -= blueDecrement
            if (blue <= 0) {
                blue = 0
            }
            result.add(Color.rgb(red, green, blue))
        }
        result.add(Color.BLACK)
        return result
    }

    /**
     * inspired by http://highintegritydesign.com/tools/tinter-shader/scripts/shader-tinter.js
     */
    fun getTints(color: Int): List<Int> {
        val result = ArrayList<Int>()
        var red = Color.red(color)
        val redIncrement = ((255 - red) * 0.1).roundToInt()
        var green = Color.green(color)
        val greenIncrement = ((255 - green) * 0.1).roundToInt()
        var blue = Color.blue(color)
        val blueIncrement = ((255 - blue) * 0.1).roundToInt()
        for (i in 0..9) {
            red += redIncrement
            if (red >= 255) {
                red = 255
            }
            green += greenIncrement
            if (green >= 255) {
                red = 255
            }
            blue += blueIncrement
            if (blue >= 255) {
                red = 255
            }
            result.add(Color.rgb(red, green, blue))
        }
        result.add(Color.WHITE)
        return result
    }

    @JvmStatic
    fun createBackgroundColorDrawable(color: Int): Drawable {
        val mask = GradientDrawable()
        mask.shape = GradientDrawable.OVAL
        mask.setColor(color)
        return mask
    }

    @JvmStatic
    fun get700Tint(color: Int): Int {
        val found = colorPrimaryDarkMap[color]
        return if (found != 0) found else color
    }

    @JvmStatic
    fun isBrightColor(color: Int): Boolean {
        return if (android.R.color.transparent == color) true else ColorUtils.calculateLuminance(
            color
        ) > 0.5
    }

    @JvmStatic
    fun getComplementColor(colorInt: Int): Int {
        val contrastColor = getBestForeground(colorInt)
        return ColorUtils.blendARGB(colorInt, contrastColor, 0.5f)
    }
}