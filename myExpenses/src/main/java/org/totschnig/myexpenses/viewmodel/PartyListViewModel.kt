package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jetbrains.annotations.Nullable
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Party

class PartyListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val parties = MutableLiveData<List<Party>>()
    fun getParties(): LiveData<List<Party>> = parties
    fun loadParties(filter: @Nullable String?, accountId: Long) {
        val filterSelection = if (TextUtils.isEmpty(filter)) null else "$KEY_PAYEE_NAME_NORMALIZED LIKE ?"
        val filterSelectionArgs = if (TextUtils.isEmpty(filter)) null else
            arrayOf("%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%")
        val accountSelection = if (accountId == 0L) null else
            StringBuilder("exists (SELECT 1 from $TABLE_TRANSACTIONS WHERE $KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID").apply {
                SelectFromMappedTableDialogFragment.accountSelection(accountId)?.let {
                    append(" AND ")
                    append(it)
                }
                append(")")
            }
        val accountSelectionArgs = if (accountId == 0L) null else SelectFromMappedTableDialogFragment.accountSelectionArgs(accountId)
        val selection = StringBuilder().apply {
            filterSelection?.let { append(it) }
            accountSelection?.let {
                if (length > 0) append(" AND ")
                append(it)
            }
        }.takeIf { it.isNotEmpty() }?.toString()
        disposable = briteContentResolver.createQuery(TransactionProvider.PAYEES_URI, null,
                selection, Utils.joinArrays(filterSelectionArgs, accountSelectionArgs), null, true)
                .mapToList { Party.fromCursor(it) }
                .subscribe {
                    parties.postValue(it)
                }
    }
}