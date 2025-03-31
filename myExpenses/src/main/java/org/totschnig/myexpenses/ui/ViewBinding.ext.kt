package org.totschnig.myexpenses.ui

import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import org.totschnig.myexpenses.databinding.ColorInputBinding
import java.util.Locale

fun ColorInputBinding.bindListener(listener: View.OnClickListener) {
    ColorEdit.setOnClickListener(listener)
}

fun ColorInputBinding.setColor(@ColorInt color: Int) {
    ColorIndicator.setBackgroundColor(color)
    ColorIndicator.contentDescription = colorToRGBString(color)
}

fun colorToRGBString(color: Int): String {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return String.format(Locale.ROOT, "RGB(%d, %d, %d)", red, green, blue)
}