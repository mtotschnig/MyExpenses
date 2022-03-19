package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme

abstract class ComposeBaseDialogFragment: BaseDialogFragment() {

    @Composable
    abstract fun BuildContent()

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setView(ComposeView(context).apply {
                setContent {
                    AppTheme(activity = requireActivity() as ProtectedFragmentActivity) {
                        BuildContent()
                    }
                }
            })
        }

    override fun onCreateDialog(savedInstanceState: Bundle?) = initBuilder().create()
}