package org.totschnig.myexpenses.ui.filter

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import com.google.android.material.chip.Chip
import org.totschnig.myexpenses.R

class ScrollingChip @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : HorizontalScrollView(context, attrs, defStyleAttr) {
    fun setOnCloseIconClickListener(removeFilter: (View) -> Unit) {
        chip.setOnCloseIconClickListener(removeFilter)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        chip.setOnClickListener(l)
    }

    var isCloseIconVisible: Boolean
        get() = chip.isCloseIconVisible
        set(value) {
            chip.isCloseIconVisible = value
        }

    var text: CharSequence
        get() = chip.text
        set(value) {
            chip.text = value
        }

    private var chip: Chip = LayoutInflater.from(context).inflate(R.layout.chip, this, false) as Chip

    init {
        addView(chip)
        attrs?.let {
            context.obtainStyledAttributes(it, intArrayOf(android.R.attr.text)).apply {
                chip.text = getText(0)
                recycle()
            }
        }
    }
}