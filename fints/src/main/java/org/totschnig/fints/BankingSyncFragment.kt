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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.dialog.ComposeBaseDialogFragment2
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.fints.R as RF

class BankingSyncFragment : ComposeBaseDialogFragment2() {
    private val viewModel: BankingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerFinTSComponent.builder()
            .appComponent((requireActivity().application as MyApplication).appComponent)
            .build()
            .inject(viewModel)
        if (savedInstanceState == null) {
            viewModel.loadBank(requireArguments().getLong(KEY_BANK_ID))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instMessage
                    .filterNotNull()
                    .collect {
                        (requireActivity() as BaseActivity).showDismissibleSnackBar(it,
                            object : Snackbar.Callback() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION ||
                                        event == DISMISS_EVENT_TIMEOUT
                                    )
                                        viewModel.messageShown()
                                }
                            })
                    }
            }
        }
    }

    @Composable
    override fun BuildContent() {
        Box(modifier = Modifier.padding(dialogPadding)) {
            val workState = viewModel.workState.collectAsState()
            val errorState = viewModel.errorState.collectAsState()
            val tanRequested = viewModel.tanRequested.observeAsState()
            val tanMediumRequested = viewModel.tanMediumRequested.observeAsState()
            val pushTanRequested = viewModel.pushTanRequested.observeAsState()
            val secMechRequested = viewModel.secMechRequested.observeAsState()

            Column {
                when (workState.value) {
                    is BankingViewModel.WorkState.BankLoaded -> {
                        val state: MutableState<BankingCredentials> = rememberSaveable {
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
                                val args = requireArguments()
                                viewModel.syncAccount(
                                    state.value,
                                    args.getLong(KEY_ACCOUNTID) to
                                    args.getLong(KEY_TYPE)
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
            TanDialog(tanRequested.value)
            TanMediaDialog(tanMediumRequested.value)
            PushTanDialog(pushTanRequested.value)
            SecMechDialog(secMechRequested.value)
        }
    }

    companion object {
        fun newInstance(bankId: Long, accountId: Long, accountTypeId: Long) = BankingSyncFragment().apply {
            arguments = Bundle().apply {
                putLong(KEY_BANK_ID, bankId)
                putLong(KEY_ACCOUNTID, accountId)
                putLong(KEY_TYPE, accountTypeId)
            }
        }
    }
}