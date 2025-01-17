package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.time.format.DateTimeParseException

const val KEY_FILTER = "filter"

class FilterPersistenceLegacy(
    val prefHandler: PrefHandler,
    private val keyTemplate: String,
) {
    private val _whereFilter: MutableStateFlow<WhereFilter>
    val whereFilter: WhereFilter
        get() = _whereFilter.value

    init {
        _whereFilter = MutableStateFlow(
            restoreFromPreferences()
        )
    }

    @CheckResult
    private fun WhereFilter.restoreColumn(
        column: String,
        producer: (String) -> SimpleCriterion<*>?
    ): WhereFilter {
        val prefNameForCriteria = prefNameForCriteria(column)
        return prefHandler.getString(prefNameForCriteria, null)?.let { prefValue ->
            producer(prefValue)?.let {
                put(it)
            } ?: kotlin.run {
                prefHandler.remove(prefNameForCriteria)
                this
            }
        } ?: this
    }

    @CheckResult
    private fun restoreFromPreferences() = WhereFilter.empty()
        .restoreColumn(KEY_CATID) {
            CategoryCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_AMOUNT) {
            AmountCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_COMMENT) {
            CommentCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_CR_STATUS) {
            CrStatusCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_PAYEEID) {
            PayeeCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_METHODID) {
            MethodCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_DATE) {
            try {
                DateCriterion.fromStringExtra(it)
            } catch (e: DateTimeParseException) {
                CrashHandler.report(e)
                null
            }
        }
        .restoreColumn(KEY_TRANSFER_ACCOUNT) {
            TransferCriterion.fromStringExtra(it)
        }
        .restoreColumn(KEY_TAGID) {
            TagCriterion.fromStringExtra(it)
        }
        .restoreColumn(ACCOUNT_COLUMN) {
            AccountCriterion.fromStringExtra(it)
        }

    private fun prefNameForCriteria(columnName: String) = keyTemplate.format(columnName)

}