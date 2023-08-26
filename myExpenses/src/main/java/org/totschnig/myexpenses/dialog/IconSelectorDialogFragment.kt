package org.totschnig.myexpenses.dialog

import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.compose.IconSelector

interface OnIconSelectedListener {
    fun onIconSelected(icon: String)
}

class IconSelectorDialogFragment : ComposeBaseDialogFragment2() {
    @Composable
    override fun BuildContent() {
        IconSelector(onIconSelected = {
            (activity as? OnIconSelectedListener)?.onIconSelected(it.key)
            dismiss()
        })
    }
}