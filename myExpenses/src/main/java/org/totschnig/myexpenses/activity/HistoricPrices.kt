package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.mainScreenPadding
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.HistoricPricesViewModel
import org.totschnig.myexpenses.viewmodel.Price
import java.security.SecureRandom
import java.text.DecimalFormat
import java.time.LocalDate

class HistoricPrices : ProtectedFragmentActivity() {

    val viewModel: HistoricPricesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.composeView.setContent {
            AppTheme {
                PriceListScreen(
                    viewModel.prices.collectAsState(initial = mapOf(LocalDate.now() to null)).value,
                    Modifier.padding(horizontal = mainScreenPadding)
                )
            }
        }
    }
}

@Composable
fun PriceListScreen(
    prices:  Map<LocalDate, Price?>,
    modifier: Modifier = Modifier
) {
    val column1Weight = .4f
    val column2Weight = .4f
    val column3Weight = .2f
    val format = remember { DecimalFormat("#.#####") }
    Column(modifier = modifier) {
        Row {
            TableCell(stringResource(R.string.date), column1Weight, true)
            TableCell("Value", column2Weight, true)
            TableCell("Source", column3Weight, true)
        }
        HorizontalDivider()
        LazyColumn {
            items(
                items = prices.entries.toList() ,
            ) { price ->
                Row {
                    TableCell(LocalDateFormatter.current.format(price.key), column1Weight)
                    TableCell(format.format(price.value?.value ?: 0), column2Weight)
                    TableCell(price.value?.source ?: "missing", column3Weight)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(8.dp),
        textAlign = TextAlign.Center,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Preview
@Composable
fun HistoricPricesPreview() {
    val  random = remember { SecureRandom() }
    PriceListScreen(buildMap {
        repeat(250) {
            val date = LocalDate.now().minusDays(it.toLong())
            put(date, Price(date, "random", random.nextDouble()))
        }
    })
}
