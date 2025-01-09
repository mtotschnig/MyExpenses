package org.totschnig.myexpenses.provider.filter

import android.os.Bundle
import androidx.core.os.BundleCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.totschnig.myexpenses.preference.PrefHandler
import timber.log.Timber

class FilterPersistenceV2(
    val prefHandler: PrefHandler,
    val prefKey: String,
    savedInstanceState: Bundle? = null,
    val immediatePersist: Boolean = true,
    restoreFromPreferences: Boolean = true
) {

    val whereFilterAsFlow: StateFlow<Criterion?>
        get() = _whereFilter
    private val _whereFilter: MutableStateFlow<Criterion?>
    val whereFilter: Criterion?
        get() = _whereFilter.value

    init {
        _whereFilter = MutableStateFlow(savedInstanceState
            ?.let { BundleCompat.getParcelable(it, KEY_FILTER, Criterion::class.java) }
            ?: if (restoreFromPreferences) restoreFromPreferences() else null)
    }

    private fun restoreFromPreferences(): Criterion? {
        return prefHandler.getString(prefKey)?.let { Json.decodeFromString<Criterion>(it) }
    }

    fun addCriterion(criterion: SimpleCriterion<*>) {
        _whereFilter.update {
            when (it) {
                null -> criterion
                is ComplexCriterion -> AndCriterion(it.criteria + criterion)
                else -> AndCriterion(listOf(it, criterion))
            }.also {
                if (immediatePersist) {
                    prefHandler.putString(prefKey, Json.encodeToString(it).also {
                        Timber.d("Filter : %s", it)
                    })
                }
            }
        }
    }
}