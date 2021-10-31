package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.time.LocalDate

class DebtOverview : ProtectedFragmentActivity() {
    val viewModel: DebtViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadDebts()
        setContent {
            MdcTheme {
                DebtList(viewModel.getDebts().observeAsState(emptyList()))
            }
        }
    }
}


@Composable
fun DebtList(debts: State<List<Debt>>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.title_activity_debt_overview)) }) }
    ) {
        LazyColumn {
            items(items = debts.value) { item ->
                Debt(debt = item)
            }
        }
    }
}

@Composable
fun Debt(debt: Debt) {
    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = debt.label
            )
            Row() {
                Text(
                    text = debt.payeeName!!,

                )
                Text(
                    text = debt.amount.toString()
                )
            }
        }
    }
}

@Preview
@Composable
fun SingleDebtPreview() {
    Debt(
        Debt(
            id = 1,
            label = "Debt 1",
            description = "some description",
            payeeId = -1L,
            amount = 4000,
            currency = "EUR",
            date = localDate2Epoch(LocalDate.now()),
            payeeName = "Joe Doe"
        )
    )
}