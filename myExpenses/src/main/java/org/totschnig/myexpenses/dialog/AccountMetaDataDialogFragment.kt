package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.sync.json.AccountMetaData

class AccountMetaDataDialogFragment: BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val data = requireArguments().getParcelable<AccountMetaData>(KEY_DATA)!!
        return initBuilder().also {
            dialogView = ComposeView(requireContext()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MdcTheme {
                        AccountMetaData(data)
                    }
                }
            }
            it.setView(dialogView)
            it.setTitle(data.label())
        }.create()
    }

    @Composable
    private fun AccountMetaData(data: AccountMetaData) {
        Column(modifier = Modifier.padding(
            horizontal = dimensionResource(id = R.dimen.padding_dialog_side),
            vertical = dimensionResource(id = R.dimen.padding_dialog_content_top)
        
        )) {
            data.description().takeIf { it.isNotEmpty() }?.let {
                AccountMetaDataRow(R.string.description, it)
            }
            AccountMetaDataRow(label = R.string.currency, value = data.currency())
            AccountMetaDataRow(label = R.string.type, value = stringResource(
                try {
                    AccountType.valueOf(data.type())
                } catch (e: Exception) {
                    AccountType.CASH
                }.toStringRes()))
            AccountMetaDataRow(label = R.string.uuid, value = data.uuid())
        }
    }

    @Composable
    private fun AccountMetaDataRow(@StringRes label: Int, value: String) {
        Row {
            Text(
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                text = stringResource(id = label)
            )
            Text(
                modifier = Modifier.weight(2f),
                text = value
            )
        }
    }

    companion object {
        const val KEY_DATA = "data"
        fun newInstance(accountMetaData: AccountMetaData) =
            AccountMetaDataDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_DATA, accountMetaData)
                }
            }
    }
}