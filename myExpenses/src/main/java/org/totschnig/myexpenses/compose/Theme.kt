package org.totschnig.myexpenses.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.getDateTimeFormatter

@Composable
fun AppTheme(
    activity: ProtectedFragmentActivity,
    content: @Composable () -> Unit
) {
    MdcTheme {
        CompositionLocalProvider(
            LocalAmountFormatter provides { amount, currency ->
                activity.currencyFormatter.convAmount(amount, activity.currencyContext[currency])
            },
            LocalDateFormatter provides getDateTimeFormatter(activity),
            content = content
        )
    }
}