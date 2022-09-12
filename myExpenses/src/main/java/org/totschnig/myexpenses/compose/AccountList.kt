package org.totschnig.myexpenses.compose

import android.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.burnoo.compose.rememberpreference.rememberBooleanPreference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.Account
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyUnit

const val EXPANSION_PREF_PREFIX = "ACCOUNT_EXPANSION_"

@Composable
fun AccountList(accountData: State<List<Account>>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        accountData.value.forEach { account ->
            item {
                //TODO migrate from legacy preferences
                val isExpanded = rememberBooleanPreference(
                    keyName = EXPANSION_PREF_PREFIX + when {
                        account.id > 0 -> account.id
                        account.id == AggregateAccount.HOME_AGGREGATE_ID -> AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE
                        else -> account.currency.code
                    },
                    initialValue = null,
                    defaultValue = true
                )
                AccountCard(account, isExpanded, account.id == 1L)
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    isExpanded: MutableState<Boolean?>,
    isSelected: Boolean = false
) {
    val format = LocalAmountFormatter.current
    isExpanded.value?.let {
        Column(
            modifier = (if (isSelected)
                Modifier.background(colorResource(id = R.color.activatedBackground))
            else Modifier).padding(
                start = dimensionResource(id = R.dimen.drawer_padding)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorCircle(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(dimensionResource(id = R.dimen.account_color_diameter_compose)),
                    color = account.color
                )
                if (account.sealed) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        modifier = Modifier.padding(end = 4.dp),
                        contentDescription = stringResource(
                            id = R.string.content_description_closed
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.label)
                    AnimatedVisibility(visible = !it) {
                        Text(
                            text = format(account.currentBalance, account.currency)
                        )
                    }

                }
                ExpansionHandle(isExpanded = it) {
                    isExpanded.value = !it
                }
            }
            AnimatedVisibility(visible = it) {
                Column(modifier = Modifier.padding(end = 16.dp)) {

                    Text(account.description)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.opening_balance))
                        Text(
                            text = format(account.openingBalance, account.currency)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.sum_income))
                        Text(
                            text = format(account.sumIncome, account.currency)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.sum_expenses))
                        Text(
                            text = format(account.sumExpense, account.currency)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.sum_transfer))
                        Text(
                            text = format(account.sumTransfer, account.currency)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.current_balance))
                        Text(
                            text = format(account.currentBalance, account.currency)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AccountPreview() {
    val isExpanded = remember {
        mutableStateOf<Boolean?>(true)
    }
    AccountCard(
        account = Account(
            id = 1,
            label = "Account",
            description = "Description",
            currency = CurrencyUnit.DebugInstance,
            color = Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true
        ),
        isExpanded = isExpanded
    )
}