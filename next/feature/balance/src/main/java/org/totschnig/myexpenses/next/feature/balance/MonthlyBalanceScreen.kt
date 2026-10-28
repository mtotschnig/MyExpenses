package org.totschnig.myexpenses.next.feature.balance

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.absoluteValue
import org.totschnig.myexpenses.designsystem.Shapes
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.DetailScaffold
import org.totschnig.myexpenses.designsystem.Dot
import org.totschnig.myexpenses.designsystem.PageSubtitle
import org.totschnig.myexpenses.designsystem.RowDivider
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.periodLabel
import org.totschnig.myexpenses.next.data.balance.BalancePeriod
import org.totschnig.myexpenses.next.data.balance.MonthlyBalance

/**
 * Breakdown of the balance of the current period: income, contracts, savings, other expenses
 * and what is left. Each row leads to where the amount comes from.
 *
 * @param onOpenSavings null, if there is nothing to show
 * @param onOpenOther null, if there is nothing to show
 * @param salaryName names of the salaries the period is based on, null for the calendar month
 * @param onChangeSalary lets the user choose the salary
 * @param hasIncomeNews unread news about regular incomes, shown as dot on the income
 */
@Composable
fun MonthlyBalanceScreen(
    balance: MonthlyBalance,
    today: LocalDate,
    currency: CurrencyInfo,
    onBack: () -> Unit,
    onOpenIncome: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenSavings: (() -> Unit)?,
    onOpenOther: (() -> Unit)?,
    salaryName: String?,
    onChangeSalary: () -> Unit,
    modifier: Modifier = Modifier,
    hasIncomeNews: Boolean = false,
) {
    val formatter = LocalAmountFormatter.current
    val colors = LocalSemanticColors.current
    fun signed(amount: Long) = when {
        amount > 0 -> "+ "
        amount < 0 -> "− "
        else -> ""
    } + formatter.format(amount.absoluteValue, currency)

    DetailScaffold(
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.next_balance_details),
        subtitle = {
            PageSubtitle(balance.period.label())
            // What the period is based on, tapping it changes the salary
            Row(
                modifier = Modifier
                    .clip(Shapes.chip)
                    .clickable(
                        onClickLabel = stringResource(R.string.next_salary),
                        onClick = onChangeSalary
                    )
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    salaryName?.let { stringResource(R.string.next_balance_salary, it) }
                        ?: stringResource(R.string.next_balance_calendar_month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
        }
    ) {
        DetailCard {
            BalanceRow(
                stringResource(R.string.next_balance_income),
                signed(balance.income),
                colors.income,
                hasNews = hasIncomeNews,
                hint = if (balance.incomeUpcoming != 0L) stringResource(
                    R.string.next_balance_income_upcoming,
                    formatter.format(balance.incomeUpcoming, currency)
                ) else null,
                onClick = onOpenIncome
            )
            BalanceRow(
                stringResource(R.string.next_tab_contracts),
                signed(balance.contracts),
                colors.expense,
                hint = if (balance.contractsUpcoming != 0L) stringResource(
                    R.string.next_balance_contracts_upcoming,
                    formatter.format(-balance.contractsUpcoming, currency)
                ) else null,
                onClick = onOpenContracts
            )
            BalanceRow(
                stringResource(R.string.next_balance_savings),
                signed(balance.reserves),
                // Money put aside is no loss
                MaterialTheme.colorScheme.onSurface,
                hint = if (balance.savingsUpcoming != 0L) stringResource(
                    R.string.next_balance_contracts_upcoming,
                    formatter.format(-balance.savingsUpcoming, currency)
                ) else null,
                onClick = onOpenSavings
            )
            BalanceRow(
                stringResource(R.string.next_balance_other),
                signed(balance.other),
                colors.expense,
                onClick = onOpenOther
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, thickness = 2.dp)
            AvailableRow(balance, today, currency)
        }
    }
}

@Composable
internal fun BalancePeriod.label(): String = periodLabel(start, end)

@Composable
private fun BalanceRow(
    label: String,
    amount: String,
    color: Color,
    onClick: (() -> Unit)?,
    hint: String? = null,
    hasNews: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (hasNews) Dot(Modifier.padding(start = 8.dp))
            }
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        // Keeps the amounts aligned, also for rows without target
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (onClick != null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
        )
    }
    RowDivider()
}

@Composable
private fun AvailableRow(balance: MonthlyBalance, today: LocalDate, currency: CurrencyInfo) {
    val formatter = LocalAmountFormatter.current
    val colors = LocalSemanticColors.current
    val color = if (balance.available < 0) colors.expense else colors.income
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            stringResource(R.string.next_balance_available_total),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatter.format(balance.available, currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (balance.available > 0) {
                Surface(
                    shape = Shapes.chip,
                    color = color.copy(alpha = 0.15f),
                    contentColor = color,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.next_balance_per_day,
                            formatter.format(balance.available / balance.period.daysLeft(today), currency)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 600)
@Preview(name = "Dark", showBackground = true, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MonthlyBalanceScreenPreview() {
    NextPreviewTheme {
        MonthlyBalanceScreen(
            balance = previewBalance(),
            today = LocalDate.now(),
            currency = CurrencyInfo.Preview,
            onBack = {},
            onOpenIncome = {},
            onOpenContracts = {},
            onOpenSavings = {},
            onOpenOther = {},
            salaryName = "Arbeitgeber GmbH",
            onChangeSalary = {}
        )
    }
}
