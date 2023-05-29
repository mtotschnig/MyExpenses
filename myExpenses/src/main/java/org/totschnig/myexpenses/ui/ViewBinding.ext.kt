package org.totschnig.myexpenses.ui

import android.content.Intent
import android.view.View
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.activity.SELECT_TAGS_REQUEST
import org.totschnig.myexpenses.databinding.ColorInputBinding
import org.totschnig.myexpenses.databinding.TagRowBinding
import org.totschnig.myexpenses.viewmodel.TagListViewModel

fun ColorInputBinding.bindListener(listener: View.OnClickListener) {
    ColorIndicator.setOnClickListener(listener)
    ColorEdit.setOnClickListener(listener)
}