package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.MultiChoiceSheet
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.SalaryChoice
import org.totschnig.myexpenses.next.data.contracts.automaticSalary
import org.totschnig.myexpenses.next.data.contracts.salaries

/**
 * Lets the user choose which regular incomes are salaries, e.g. for more than one job.
 * Without any, the balance uses the calendar month.
 *
 * @param incomes the regular incomes, without dismissed ones
 */
@Composable
fun SalarySheet(
    incomes: List<Contract>,
    choice: SalaryChoice,
    onSelect: (SalaryChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = incomes.salaries(choice).map { it.signature }
    var selected by rememberSaveable { mutableStateOf(current) }
    val automatic = incomes.automaticSalary()
    MultiChoiceSheet(
        title = stringResource(R.string.next_salary),
        options = incomes.filter { it.isIncome && it.isActive },
        selected = incomes.filterTo(HashSet()) { it.signature in selected },
        onToggle = { income ->
            selected = if (income.signature in selected) selected - income.signature else selected + income.signature
        },
        label = { it.title },
        onConfirm = {
            when {
                // Unchanged: keep following the automatic choice
                selected.toSet() == current.toSet() -> onDismiss()
                selected.isEmpty() -> onSelect(SalaryChoice.None)
                else -> onSelect(SalaryChoice.Fixed(selected.toSet()))
            }
        },
        onDismiss = onDismiss,
        intro = stringResource(R.string.next_salary_explanation),
        footer = {
            Text(
                stringResource(R.string.next_salary_none_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (choice != SalaryChoice.Automatic) {
                TextButton(onClick = { onSelect(SalaryChoice.Automatic) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        stringResource(
                            R.string.next_salary_automatic,
                            automatic?.displayName ?: stringResource(R.string.next_salary_none_detected)
                        )
                    )
                }
            }
        }
    )
}
