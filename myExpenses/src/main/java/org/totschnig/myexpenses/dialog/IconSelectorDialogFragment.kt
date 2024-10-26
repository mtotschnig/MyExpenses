package org.totschnig.myexpenses.dialog

import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.compose.IconSelector

interface OnIconSelectedListener {
    fun onIconSelected(icon: String)
}

class IconSelectorDialogFragment : ComposeBaseDialogFragment2() {

    override val fullScreenIfNotLarge = true
    @Composable
    override fun BuildContent() {
        IconSelector(onIconSelected = {
            (activity as? OnIconSelectedListener)?.onIconSelected(it)
            dismiss()
        })
    }
}