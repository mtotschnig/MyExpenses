package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import org.totschnig.myexpenses.activity.DebtActivity
import org.totschnig.myexpenses.compose.DebtRenderer
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import javax.inject.Inject

class DebtDetailsDialogFragment : ComposeBaseDialogFragment() {

    private lateinit var viewModel: DebtViewModel

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    val debt by lazy { viewModel.loadDebt(requireArguments().getLong(DatabaseConstants.KEY_DEBT_ID)) }

    @Composable
    override fun BuildContent() {
        debt.collectAsState(null).value?.let { debt ->
                val debtActivity = requireActivity() as DebtActivity
                DebtRenderer(
                    debt = debt,
                    transactions = viewModel.loadTransactions(debt).observeAsState(emptyList()).value,
                    expanded = true,
                    onEdit = { debtActivity.editDebt(debt) },
                    onDelete = { count -> debtActivity.deleteDebt(debt, count) },
                    onToggle = { debtActivity.toggleDebt(debt) },
                    onShare = { exportFormat -> debtActivity.shareDebt(debt, exportFormat, snackBarContainer) },
                    onTransactionClick = { showDetails(it) }
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(injector) {
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
