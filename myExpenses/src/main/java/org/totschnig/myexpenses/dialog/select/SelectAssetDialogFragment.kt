package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_IS_PORTFOLIO
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.PORTFOLIO_ASSET
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.AssetCriterion
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION

class SelectAssetDialogFragment :
    SelectFilterDialog<AssetCriterion>(false, AssetCriterion::class.java) {
    override fun makeCriteria(label: String, vararg ids: Long) = AssetCriterion(label, *ids)
    override val uri: Uri = TransactionProvider.ACCOUNTS_BASE_URI
    override val column: String = KEY_LABEL

    override val dialogTitle: Int
        get() = R.string.search_asset
    override val selection: String
        get() = "$KEY_PARENTID = ? AND $KEY_IS_PORTFOLIO = $PORTFOLIO_ASSET"
    override val selectionArgs: Array<String>
        get() = arrayOf(requireArguments().getLong(KEY_PARENTID).toString())

    companion object {
        fun newInstance(
            requestKey: String,
            portfolioId: Long,
            criterion: AssetCriterion?
        ) =
            SelectAssetDialogFragment().apply {
                arguments = configureArguments(requestKey).apply {
                    putLong(KEY_PARENTID, portfolioId)
                    putParcelable(KEY_CRITERION, criterion)
                }
            }
    }
}
