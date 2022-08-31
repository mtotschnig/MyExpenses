package org.totschnig.myexpenses.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.util.UiUtils
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
                activity.currencyFormatter.convAmount(amount, currency)
            },
            LocalDateFormatter provides getDateTimeFormatter(activity),
            LocalColors provides Colors(
                income = colorResource(id = R.color.colorIncome),
                expense = colorResource(id = R.color.colorExpense),
                iconTint = Color(UiUtils.getColor(activity, R.attr.colorControlNormal))
            )
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2, content = content)
        }
    }
}