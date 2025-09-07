package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.compose.CompactTransactionRenderer
import org.totschnig.myexpenses.compose.DateTimeFormatInfo
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.ui.asDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.KEY_LOADING_INFO
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import java.text.SimpleDateFormat
import javax.inject.Inject

class TransactionListComposeDialogFragment : ComposeBaseDialogFragment(),
    DialogInterface.OnClickListener {

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    val viewModel by viewModels<TransactionListViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(injector) {
            inject(this@TransactionListComposeDialogFragment)
            inject(viewModel)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val loadingInfo = viewModel.loadingInfo
                viewModel.sum.collect {
                    val title = loadingInfo.label + TABS +
                            currencyFormatter.convAmount(it, loadingInfo.currency)
                    dialog!!.setTitle(title)
                }
            }
        }
    }

    override fun initBuilder(): AlertDialog.Builder {
        val loadingInfo = viewModel.loadingInfo
        return super.initBuilder().apply {
            setTitle(loadingInfo.label)
            setPositiveButton(
                if (loadingInfo.withNewButton) R.string.menu_create_transaction else android.R.string.ok,
                if (loadingInfo.withNewButton)
                    this@TransactionListComposeDialogFragment else null
            )
            loadingInfo.icon?.let {
                IIconInfo.resolveIcon(it)?.asDrawable(requireContext())
            }?.let { setIcon(it) }
        }
    }

    @Composable
    override fun BuildContent() {
        val data = viewModel.transactions.collectAsState(initial = emptyList())
        val renderer = CompactTransactionRenderer(
            dateTimeFormatInfo = DateTimeFormatInfo(
                (Utils.ensureDateFormatWithShortYear(context) as SimpleDateFormat).asDateTimeFormatter,
                4.6f
            ),
            withCategoryIcon = false,
            withOriginalAmount = prefHandler.getBoolean(
                PrefKey.UI_ITEM_RENDERER_ORIGINAL_AMOUNT,
                false
            )
        )
        LazyColumn(
            modifier = Modifier.padding(
                top = dialogPadding,
                start = dialogPadding,
                end = dialogPadding
            )
        ) {
            data.value.forEach {
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable {
                                showDetails(it.parentId ?: it.id)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        with(renderer) {
                            RenderInner(transaction = it)
                        }
                    }
                }
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        with(viewModel.loadingInfo) {
            startActivity(
                Intent(requireContext(), ExpenseEdit::class.java).apply {
                    putExtra(KEY_ACCOUNTID, accountId)
                    putExtra(KEY_CURRENCY, currency)
                    putExtra(KEY_COLOR, color)
                }
            )
        }
    }

    companion object {
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