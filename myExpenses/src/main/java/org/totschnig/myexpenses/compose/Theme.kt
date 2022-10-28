package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.getDateTimeFormatter

@Composable
fun AppTheme(
    context: Context,
    content: @Composable () -> Unit
) {
    MdcTheme {
        CompositionLocalProvider(
            LocalCurrencyFormatter provides (context.applicationContext as MyApplication).appComponent.currencyFormatter(),
            LocalDateFormatter provides getDateTimeFormatter(context),
            LocalColors provides Colors(
                income = colorResource(id = R.color.colorIncome),
                expense = colorResource(id = R.color.colorExpense),
                transfer = colorResource(id = R.color.colorTransfer),
            )
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2, content = content)
        }
    }
}