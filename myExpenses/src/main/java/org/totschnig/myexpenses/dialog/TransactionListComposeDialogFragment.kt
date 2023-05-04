package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.compose.CompactTransactionRenderer
import org.totschnig.myexpenses.compose.mainScreenPadding
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.asDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconInfo
import java.text.SimpleDateFormat

class TransactionListComposeDialogFragment: ComposeBaseDialogFragment() {

    val viewModel by viewModels<TransactionListViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
    }

    override fun initBuilder(): AlertDialog.Builder = super.initBuilder().apply {
        setTitle(viewModel.loadingInfo.label)
        setPositiveButton(android.R.string.ok, null)
        viewModel.loadingInfo.icon?.let {
            IIconInfo.resolveIcon(it)?.asDrawable(requireContext())
        }?.let { setIcon(it) }
    }

    @Composable
    override fun BuildContent() {
        val data = viewModel.loadTransactions().collectAsState(initial = emptyList())
        val renderer = CompactTransactionRenderer(
            dateTimeFormatInfo = Pair(
                (Utils.ensureDateFormatWithShortYear(context) as SimpleDateFormat).asDateTimeFormatter,
                with(LocalDensity.current) {
                    LocalTextStyle.current.fontSize.toDp()
                } * 4.6f
            ),
            withCategoryIcon = false,
            horizontalPadding = 12.dp

        )
        LazyColumn {
            data.value.forEach {
                item {
                    renderer.Render(transaction = it)
                }
            }
        }
    }

    companion object {
        private const val KEY_LOADING_INFO = "loadingInfo"
        private const val TABS = "\u0009\u0009\u0009\u0009"

        @JvmStatic
        fun newInstance(
            loadingInfo: TransactionListViewModel.LoadingInfo
        ) = TransactionListComposeDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_LOADING_INFO, loadingInfo)
            }
        }
    }
}