package org.totschnig.myexpenses.next.feature.balance

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.Shapes
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.LoadingBlock
import org.totschnig.myexpenses.designsystem.OverviewCard
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme
import org.totschnig.myexpenses.next.core.common.R
import java.time.LocalDate
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.data.balance.BalanceOverview
import org.totschnig.myexpenses.next.data.balance.BalancePeriod
import org.totschnig.myexpenses.next.data.balance.MonthlyBalance

/**
 * Card of the overview: income and expenses of the current salary cycle and what is left.
 * Before transactions may be analysed, it asks for consent. Shows nothing when declined, and its
 * outline while loading, so that the cards below do not jump.
 */
@Composable
fun MonthlyBalanceCard(
    state: AnalysisState<BalanceOverview>,
    currency: CurrencyInfo,
    onOpen: () -> Unit,
    onConsent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AnalysisState.Declined -> {}
        AnalysisState.Loading -> LoadingBalanceCard(modifier)
        AnalysisState.AskConsent -> ConsentCard(onConsent, modifier)
        is AnalysisState.Ready -> BalanceCard(state.value.balance, state.value.today, currency, onOpen, modifier)
    }
}

@Composable
private fun ConsentCard(onConsent: () -> Unit, modifier: Modifier) {
    OverviewCard(stringResource(R.string.next_balance_title), modifier) {
        Text(
            stringResource(R.string.next_balance_consent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onConsent, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.next_contracts_consent_accept))
        }
    }
}

/** As high as [BalanceCard] */
@Composable
private fun LoadingBalanceCard(modifier: Modifier) {
    OverviewCard(stringResource(R.string.next_balance_title), modifier) {
        LoadingBlock(Modifier.fillMaxWidth().padding(top = 4.dp), height = 28.dp)
        LoadingBlock(Modifier.fillMaxWidth().padding(top = 8.dp), height = 28.dp)
        LoadingBlock(Modifier.fillMaxWidth(0.5f).padding(top = 16.dp), height = 36.dp)
        LoadingBlock(Modifier.fillMaxWidth(0.4f).padding(top = 4.dp))
    }
}

@Composable
private fun BalanceCard(
    balance: MonthlyBalance,
    today: LocalDate,
    currency: CurrencyInfo,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val formatter = LocalAmountFormatter.current
    val colors = LocalSemanticColors.current
    val max = maxOf(balance.income, balance.expenses, 1)
    val daysLeft = balance.period.daysLeft(today).toInt()
    OverviewCard(stringResource(R.string.next_balance_title), modifier, onClick = onOpen) {
        AmountBar(
            label = stringResource(R.string.next_balance_income),
            amount = formatter.format(balance.income, currency),
            fraction = balance.income.toFloat() / max,
            color = colors.income,
            modifier = Modifier.padding(top = 4.dp)
        )
        AmountBar(
            label = stringResource(R.string.next_balance_expenses),
            amount = formatter.format(balance.expenses, currency),
            fraction = balance.expenses.toFloat() / max,
            color = colors.expense,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                formatter.format(balance.available, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance.available < 0) colors.expense else MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.next_balance_available),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        Text(
            pluralStringResource(
                if (balance.period.isSalaryCycle) R.plurals.next_balance_until_salary
                else R.plurals.next_balance_until_month_end,
                daysLeft, daysLeft
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tinted bar whose width shows the amount in relation to the larger one of income and expenses
 */
@Composable
private fun AmountBar(
    label: String,
    amount: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                .clip(Shapes.chip)
                .background(color.copy(alpha = 0.15f))
                .padding(vertical = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                modifier = Modifier.weight(1f)
            )
            Text(
                amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

internal fun previewBalance() = MonthlyBalance(
    period = BalancePeriod(LocalDate.now().minusDays(22), LocalDate.now().plusDays(8), true),
    incomeBooked = 153684,
    incomeUpcoming = 0,
    contractsBooked = -70000,
    contractsUpcoming = -12526,
    savings = -35000,
    other = -10643,
    incomeTransactionIds = emptyList(),
    savingsTransactionIds = listOf(1),
    otherTransactionIds = listOf(2)
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MonthlyBalanceCardPreview() {
    NextPreviewTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MonthlyBalanceCard(
                state = AnalysisState.Ready(BalanceOverview(previewBalance(), LocalDate.now())),
                currency = CurrencyInfo.Preview,
                onOpen = {},
                onConsent = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
