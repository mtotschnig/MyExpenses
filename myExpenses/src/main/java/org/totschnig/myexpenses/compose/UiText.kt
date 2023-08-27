package org.totschnig.myexpenses.compose

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

//https://stackoverflow.com/a/74044617/1199911
sealed interface UiText {

    @JvmInline
    value class StringValue(val str: String) : UiText
    @JvmInline
    value class StringResource(@StringRes val resourceId: Int) : UiText

    class StringResourceWithArgs(
        @StringRes val resourceId: Int,
        vararg val args: Any
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is StringValue -> str
            is StringResource -> stringResource(id = resourceId)
            is StringResourceWithArgs -> stringResource(id = resourceId, formatArgs = args)
        }
    }
}