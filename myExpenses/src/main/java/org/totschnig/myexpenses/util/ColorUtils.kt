package org.totschnig.myexpenses.util

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.utils.ColorTemplate
import org.totschnig.myexpenses.util.ui.getBestForeground
import kotlin.math.roundToInt

object ColorUtils {
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

    fun isBrightColor(color: Int) =
        if (android.R.color.transparent == color)
            true
        else
            ColorUtils.calculateLuminance(color) > 0.5

    fun getComplementColor(colorInt: Int): Int {
        val contrastColor = getBestForeground(colorInt)
        return ColorUtils.blendARGB(colorInt, contrastColor, 0.5f)
    }
}