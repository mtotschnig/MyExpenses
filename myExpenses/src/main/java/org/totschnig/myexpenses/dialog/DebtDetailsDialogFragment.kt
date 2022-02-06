package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.DebtActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.DebtRenderer
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject

class DebtDetailsDialogFragment : BaseDialogFragment() {

    val viewModel: DebtViewModel
        get() = (requireActivity() as DebtActivity).debtViewModel

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().applicationContext as MyApplication).appComponent) {
            inject(this@DebtDetailsDialogFragment)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = with(initBuilder()) {
        setView(ComposeView(context).apply {
            setContent {
                AppTheme(activity = requireActivity() as ProtectedFragmentActivity) {
                    viewModel.loadDebt(requireArguments().getLong(DatabaseConstants.KEY_DEBT_ID))
                        .observeAsState().value?.let<Debt, Unit> { debt ->
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
            }
        })
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        fun newInstance(debtId: Long) = DebtDetailsDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(DatabaseConstants.KEY_DEBT_ID, debtId)
            }
        }

    }
}
