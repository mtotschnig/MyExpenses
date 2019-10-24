package org.totschnig.myexpenses.util

import com.google.android.material.chip.ChipGroup
import org.totschnig.myexpenses.ui.filter.ScrollingChip

 fun addChipsBulk(chipGroup: ChipGroup, chips: List<String>) {
     with(chipGroup) {
         removeAllViews()
         for (chip in chips) {
             addView(ScrollingChip(context).apply {
                 text = chip
             })
         }
     }
 }