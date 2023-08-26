package org.totschnig.myexpenses.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.fragment.app.DialogFragment
import org.totschnig.myexpenses.compose.AppTheme

abstract class ComposeBaseDialogFragment2: DialogFragment() {

    val dialogPadding = 16.dp

    @Composable
    abstract fun BuildContent()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeDialogView(requireContext()).also {
            it.consumeWindowInsets = false
            it.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            it.setContent {
                AppTheme {
                    BuildContent()
                }
            }
        }
    }

    inner class ComposeDialogView(context: Context) : AbstractComposeView(context, null, 0),
        DialogWindowProvider {


        // The important bit
        override val window get() = requireDialog().window!!

        private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

        override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
            private set

        @Composable
        override fun Content() {
            content.value?.invoke()
        }

        override fun getAccessibilityClassName(): CharSequence = ComposeView::class.java.name

        /**
         * Set the Jetpack Compose UI content for this view.
         * Initial composition will occur when the view becomes attached to a window or when
         * [createComposition] is called, whichever comes first.
         */
        fun setContent(content: @Composable () -> Unit) {
            shouldCreateCompositionOnAttachedToWindow = true
            this.content.value = content
            if (isAttachedToWindow) createComposition()
        }
    }

    private var ComposeDialogView.consumeWindowInsets: Boolean
        get() = getTag(androidx.compose.ui.R.id.consume_window_insets_tag) as? Boolean ?: true
        set(value) {
            setTag(androidx.compose.ui.R.id.consume_window_insets_tag, value)
        }
}
