package org.totschnig.myexpenses.util

import com.google.android.material.chip.ChipGroup
import org.totschnig.myexpenses.ui.filter.ScrollingChip

 fun <T>ChipGroup.addChipsBulk(chips: Iterable<T>, closeFunction: ((T) -> Unit)?) {
     removeAllViews()
     for (chip in chips) {
         addView(ScrollingChip(context).also { scrollingChip ->
             scrollingChip.text = chip.toString()
             closeFunction?.let {
                 scrollingChip.isCloseIconVisible = true
                 scrollingChip.setOnCloseIconClickListener {
                     removeView(scrollingChip)
                     it(chip)
                 }
             }
         })
     }
 }