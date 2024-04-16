package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.AppTheme

abstract class ComposeBaseDialogFragment: BaseDialogFragment() {

    val dialogPadding = 24.dp

    @Composable
    abstract fun BuildContent()

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setView(ComposeView(context).apply {
                setContent {
                    AppTheme {
                        BuildContent()
                    }
                }
            })
        }

    override fun onCreateDialog(savedInstanceState: Bundle?) = initBuilder().create()
}