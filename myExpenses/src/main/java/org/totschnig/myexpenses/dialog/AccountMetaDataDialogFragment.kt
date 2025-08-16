package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel

class AccountMetaDataDialogFragment: ComposeBaseDialogFragment3() {

    val viewModel: ContentResolvingAndroidViewModel by viewModels()

    val data: AccountMetaData
        get() = BundleCompat.getParcelable(requireArguments(), KEY_DATA, AccountMetaData::class.java)!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
    }

    @Composable
    override fun ColumnScope.MainContent() {
        AccountMetaData(data)
    }

    override val title: CharSequence?
        get() = data.label()


    @Composable
    private fun AccountMetaData(data: AccountMetaData) {
        val type = AccountType.initialAccountTypes.firstOrNull() {
                it.name == data.type() || it.nameForSyncLegacy == data.type()
            }?.localizedName(LocalContext.current)
            ?: data.type()

        data.description().takeIf { it.isNotEmpty() }?.let {
            AccountMetaDataRow(R.string.description, it)
        }
        AccountMetaDataRow(label = R.string.currency, value = data.currency())
        AccountMetaDataRow(label = R.string.type, value = type)
        AccountMetaDataRow(label = R.string.uuid, value = data.uuid())
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