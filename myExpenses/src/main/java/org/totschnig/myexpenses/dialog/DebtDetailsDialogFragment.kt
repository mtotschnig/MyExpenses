package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.DebtActivity
import org.totschnig.myexpenses.compose.DebtRenderer
import org.totschnig.myexpenses.compose.LocalAmountFormatter
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject

class DebtDetailsDialogFragment : BaseDialogFragment() {

    val viewModel: DebtViewModel by viewModels()

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().applicationContext as MyApplication).appComponent) {
            inject(this@DebtDetailsDialogFragment)
            inject(viewModel)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = with(initBuilder()) {
        setView(ComposeView(context).apply {
            setContent {
                CompositionLocalProvider(
                    LocalAmountFormatter provides { amount, currency ->
                        currencyFormatter.convAmount(amount, currencyContext[currency])
                    },
                    LocalDateFormatter provides getDateTimeFormatter(requireContext())
                ) {
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
                                        onDelete = debtActivity::deleteDebt
                                    )
                                }
                        }
                }
            }
        })
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    fun deleteDebtDo(debtId: Long) {
        viewModel.deleteDebt(debtId).observe(this) {
            if (it) {
                dismiss()
            } else {
                showSnackbar("ERROR", Snackbar.LENGTH_LONG, null)
            }
        }
    }

    companion object {
        fun newInstance(debtId: Long) = DebtDetailsDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(DatabaseConstants.KEY_DEBT_ID, debtId)
            }
        }

    }
}