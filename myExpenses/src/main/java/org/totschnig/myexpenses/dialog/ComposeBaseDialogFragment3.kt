package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.AppTheme

/**
 * variant of [ComposeBaseDialogFragment] which guarantees
 * design specs.
 */
abstract class ComposeBaseDialogFragment3 : BaseDialogFragment() {

    private val bottomPadding = 24.dp
    private val titlePadding = 16.dp
    open val horizontalPadding = 24.dp

    @Composable
    abstract fun ColumnScope.MainContent()

    abstract val title: CharSequence

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setTitle(title)
            setView(ComposeView(context).apply {
                setContent {
                    AppTheme {
                        Column(
                            modifier = Modifier.padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                bottom = bottomPadding,
                                top = titlePadding
                            )
                        ) {
                            MainContent()
                        }
                    }
                }
            })
        }

    override fun onCreateDialog(savedInstanceState: Bundle?) = initBuilder().create()
}