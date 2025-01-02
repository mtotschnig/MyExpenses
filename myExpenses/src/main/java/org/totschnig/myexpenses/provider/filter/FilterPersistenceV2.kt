package org.totschnig.myexpenses.provider.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import org.totschnig.myexpenses.preference.PrefHandler

class FilterPersistenceV2(
    val prefHandler: PrefHandler,
    val prefKey: String
) {
    val whereFilterAsFlow: StateFlow<BaseCriterion?>
        get() = _whereFilter
    private val _whereFilter: MutableStateFlow<BaseCriterion?>
    val whereFilter: BaseCriterion?
        get() = _whereFilter.value

    init {
        _whereFilter = MutableStateFlow(restoreFromPreferences())
    }

    private fun restoreFromPreferences(): BaseCriterion? {
        return prefHandler.getString(prefKey)?.let { Json.decodeFromString<BaseCriterion>(it) }
    }

    fun addCriterion(criterion: Criterion<*>) {
        _whereFilter.value = criterion
    }

}