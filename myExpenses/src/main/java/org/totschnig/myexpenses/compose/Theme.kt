package org.totschnig.myexpenses.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.google.accompanist.themeadapter.material.MdcTheme
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.util.getDateTimeFormatter

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    MdcTheme {
        val injector = context.injector
        CompositionLocalProvider(
            LocalCurrencyFormatter provides injector.currencyFormatter(),
            LocalDateFormatter provides getDateTimeFormatter(context),
            LocalColors provides Colors(
                income = colorResource(id = R.color.colorIncome),
                expense = colorResource(id = R.color.colorExpense),
                transfer = colorResource(id = R.color.colorTransfer),
            ),
            LocalHomeCurrency provides injector.homeCurrencyProvider().homeCurrencyUnit,
            LocalTracker provides injector.tracker()
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2, content = content)
        }
    }
}