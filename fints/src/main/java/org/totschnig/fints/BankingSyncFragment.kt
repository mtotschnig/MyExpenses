package org.totschnig.fints

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment2
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.fints.R as RF

class BankingSyncFragment : ComposeBaseDialogFragment2() {
    private val viewModel: BankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(requireActivity().injector) {
            inject(viewModel)
        }
        viewModel.loadBank(requireArguments().getLong(KEY_BANK_ID))
    }

    @Composable
    override fun BuildContent() {
        Box(modifier = Modifier.padding(dialogPadding)) {
            val workState = viewModel.workState.collectAsState()
            val errorState = viewModel.errorState.collectAsState()
            val tanRequested = viewModel.tanRequested.observeAsState()
            val tanMediumRequested = viewModel.tanMediumRequested.observeAsState()

            Column {
                when (workState.value) {
                    is BankingViewModel.WorkState.BankLoaded -> {
                        val state: MutableState<BankingCredentials> = remember {
                            mutableStateOf(
                                BankingCredentials.fromBank((workState.value as BankingViewModel.WorkState.BankLoaded).bank)
                            )
                        }
                        BankingCredentials(bankingCredentials = state, onDone = {})
                        ButtonRow {
                            Button(onClick = { dismiss() }) {
                                Text(stringResource(id = android.R.string.cancel))
                            }
                            Button(enabled = state.value.isComplete, onClick = {
                                viewModel.syncAccount(
                                    state.value,
                                    requireArguments().getLong(KEY_ACCOUNTID)
                                )
                            }) {
                                Text(stringResource(RF.string.load))
                            }
                        }
                    }


                    is BankingViewModel.WorkState.Done -> {
                        Error(errorMessage = errorState.value)
                        (workState.value as? BankingViewModel.WorkState.Success)?.message?.let {
                            Text(it)
                        }
                        ButtonRow {
                            Button(onClick = { dismiss() }) {
                                Text(stringResource(id = R.string.menu_close))
                            }
                        }
                    }

                    is BankingViewModel.WorkState.Loading -> Loading(
                        (workState.value as BankingViewModel.WorkState.Loading).message
                    )

                    else -> {}
                }
            }
            TanDialog(tanRequest = tanRequested.value, submitTan = viewModel::submitTan)
            TanMediaDialog(options = tanMediumRequested.value, submitMedia = viewModel::submitTanMedium)
        }
    }

    companion object {
        fun newInstance(bankId: Long, accountId: Long) = BankingSyncFragment().apply {
            arguments = Bundle().apply {
                putLong(KEY_BANK_ID, bankId)
                putLong(KEY_ACCOUNTID, accountId)
            }
        }
    }
}