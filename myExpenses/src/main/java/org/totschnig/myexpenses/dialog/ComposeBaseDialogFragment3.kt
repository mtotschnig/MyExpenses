package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG_ROOT

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

    open val title: CharSequence? = null

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().apply {
            setTitle(title)
            setView(ComposeView(context).apply {
                setContent {
                    AppTheme {
                        Column(
                            modifier = Modifier
                                .testTag(TEST_TAG_DIALOG_ROOT)
                                .fillMaxWidth()
                                .padding(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    //on fullScreen 24.dp seems to much, due to System UI
                                    bottom = if (fullScreen) 0.dp else bottomPadding,
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