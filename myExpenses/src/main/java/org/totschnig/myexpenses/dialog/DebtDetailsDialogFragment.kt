package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.DebtActivity
import org.totschnig.myexpenses.compose.DebtRenderer
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import javax.inject.Inject

class DebtDetailsDialogFragment : ComposeBaseDialogFragment() {

    private lateinit var viewModel: DebtViewModel

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Composable
    override fun BuildContent() {
        viewModel.loadDebt(requireArguments().getLong(DatabaseConstants.KEY_DEBT_ID))
            .observeAsState().value?.let { debt ->
                viewModel.loadTransactions(debt)
                    .observeAsState().value?.let { transactions ->
                        val debtActivity = requireActivity() as DebtActivity
                        DebtRenderer(
                            debt = debt,
                            transactions = transactions,
                            expanded = true,
                            onEdit = debtActivity::editDebt,
                            onDelete = debtActivity::deleteDebt,
                            onToggle = debtActivity::toggleDebt,
                            onShare = debtActivity::shareDebt
                        )
                    }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().applicationContext as MyApplication).appComponent) {
            inject(this@DebtDetailsDialogFragment)
        }
        viewModel = ViewModelProvider(requireActivity())[DebtViewModel::class.java]
    }

    override fun initBuilder(): AlertDialog.Builder =
        super.initBuilder().setPositiveButton(android.R.string.ok, null)

    companion object {
        fun newInstance(debtId: Long) = DebtDetailsDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(DatabaseConstants.KEY_DEBT_ID, debtId)
            }
        }
    }
}
