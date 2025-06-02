package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.DateRangePickerScaffold
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.viewmodel.PriceHistoryViewModel

class BatchPriceDownloadDialogFragment : ComposeBaseDialogFragment2() {

    private val viewmodel: PriceHistoryViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override val fullScreenIfNotLarge = true

    private val source: ExchangeRateApi
        get() = ExchangeRateApi.getByName(requireArguments().getString(KEY_SOURCE)!!)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun BuildContent() {
        DateRangePickerScaffold(
            confirmButtonText = stringResource(R.string.batch_download),
            warning = if (source == ExchangeRateApi.Frankfurter) null else
                getString(R.string.warning_batch_download_quota, source.host),
        ) { (start, end) ->
            viewmodel.loadTimeSeries(source, start, end)
        }
    }

    companion object {
        fun newInstance(source: String) = BatchPriceDownloadDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_SOURCE, source)
            }
        }
    }
}