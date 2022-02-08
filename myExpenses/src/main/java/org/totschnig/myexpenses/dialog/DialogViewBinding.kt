package org.totschnig.myexpenses.dialog

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding

abstract class DialogViewBinding<VB : ViewBinding> : BaseDialogFragment() {
    private var _binding: VB? = null

    // This property is only valid between onCreateView and onDestroyView.
    val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun initBuilder(inflate: (LayoutInflater) -> VB): AlertDialog.Builder =
        initBuilderWithView { layoutInflater ->
            inflate(layoutInflater).also { _binding = it }.root
        }
}