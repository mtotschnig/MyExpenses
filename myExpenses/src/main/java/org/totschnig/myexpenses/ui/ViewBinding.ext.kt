package org.totschnig.myexpenses.ui

import android.view.View
import androidx.annotation.ColorInt
import eltos.simpledialogfragment.color.ColorView
import org.totschnig.myexpenses.databinding.ColorInputBinding

fun ColorInputBinding.bindListener(listener: View.OnClickListener) {
    ColorEdit.setOnClickListener(listener)
}

fun ColorInputBinding.setColor(@ColorInt color: Int) {
    ColorIndicator.setBackgroundColor(color)
    ColorIndicator.contentDescription = ColorView.colorToRGBString(color)
}