package org.totschnig.myexpenses.dialog

import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.compose.IconSelector
import org.totschnig.myexpenses.viewmodel.data.IIconInfo

interface OnIconSelectedListener {
    fun onIconSelected(icon: String)
}

class IconSelectorDialogFragment : ComposeBaseDialogFragment() {
    @Composable
    override fun BuildContent() {
        IconSelector(onIconSelected = {
            (activity as? OnIconSelectedListener)?.onIconSelected(it.key)
            dismiss()
        })
    }
}