package org.totschnig.myexpenses.next.data.contracts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The decisions of the user about contracts and the news about them, stored in the data store,
 * which is part of the backup
 */
class ContractStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<ContractSettings> = dataStore.data.map { it.toContractSettings() }

    /** Newest first, see [ContractNews] */
    val news: Flow<List<ContractNews>> = dataStore.data.map { it.news() }

    /** Changes several values at once, e.g. the rules together with the salary */
    suspend fun edit(block: (Editor) -> Unit) {
        dataStore.edit { block(Editor(it)) }
    }

    class Editor internal constructor(private val preferences: MutablePreferences) {
        val settings: ContractSettings get() = preferences.toContractSettings()

        var rules: List<ContractRule>
            get() = preferences[KEY_RULES]?.let(::parseRules) ?: emptyList()
            set(value) {
                preferences[KEY_RULES] = serializeRules(value)
            }

        var news: List<ContractNews>
            get() = preferences.news()
            set(value) {
                preferences[KEY_NEWS] = serializeNews(value)
            }

        var customAreas: List<CustomArea>
            get() = preferences.customAreas()
            set(value) {
                preferences[KEY_CUSTOM_AREAS] = serializeCustomAreas(value)
            }

        fun setSalary(choice: SalaryChoice) {
            preferences.remove(KEY_SALARY)
            preferences.remove(KEY_SALARIES)
            when (choice) {
                SalaryChoice.Automatic -> {}
                SalaryChoice.None -> preferences[KEY_SALARY] = SALARY_NONE
                is SalaryChoice.Fixed -> preferences[KEY_SALARIES] = choice.signatures
            }
        }

        /** The salary [old] is now part of the contract with signature [new] */
        fun replaceSalary(old: String, new: String) {
            preferences[KEY_SALARIES]?.takeIf { old in it }?.let { preferences[KEY_SALARIES] = it - old + new }
        }

        /**
         * The decisions stored by signature before there were rules have been taken over into the
         * rules, see [ContractSettings.rulesMigrated]
         *
         * @param dropOld whether to remove them
         */
        fun markRulesMigrated(dropOld: Boolean) {
            if (dropOld) {
                preferences.remove(KEY_DISMISSED)
                preferences.remove(KEY_NAMES)
                preferences.remove(KEY_AREAS)
            }
            preferences[KEY_RULES_MIGRATED] = true
        }

        /** Takes back a dismissal stored by signature before there were rules */
        fun restoreDismissedBySignature(signature: String) {
            preferences[KEY_DISMISSED]?.let { preferences[KEY_DISMISSED] = it - signature }
        }

        /** Contracts put into the category [key] by signature go back to their suggested category */
        fun removeAreaBySignature(key: String) {
            preferences[KEY_AREAS]?.let(::parseMap)?.let { areas ->
                preferences[KEY_AREAS] = serializeMap(areas.filterValues { it != key })
            }
        }
    }

    private companion object {
        val KEY_DISMISSED = stringSetPreferencesKey("next_contracts_dismissed")
        /** JSON object mapping signatures to custom names */
        val KEY_NAMES = stringPreferencesKey("next_contracts_names")
        /** JSON object mapping signatures to the key of a [ContractArea] or [ContractSettings.AREA_NONE] */
        val KEY_AREAS = stringPreferencesKey("next_contracts_areas")
        /** JSON array of the categories created by the user */
        val KEY_CUSTOM_AREAS = stringPreferencesKey("next_contracts_custom_areas")
        /** [SALARY_NONE] for the calendar month */
        val KEY_SALARY = stringPreferencesKey("next_contracts_salary")
        /** Signatures of the incomes chosen as salaries, missing for the automatic choice */
        val KEY_SALARIES = stringSetPreferencesKey("next_contracts_salaries")
        /** JSON array of [ContractRule]s */
        val KEY_RULES = stringPreferencesKey("next_contracts_rules")
        val KEY_RULES_MIGRATED = booleanPreferencesKey("next_contracts_rules_migrated")
        /** JSON array of [ContractNews] */
        val KEY_NEWS = stringPreferencesKey("next_contracts_news")
        const val SALARY_NONE = "NONE"

        fun Preferences.toContractSettings() = ContractSettings(
            dismissed = this[KEY_DISMISSED] ?: emptySet(),
            names = this[KEY_NAMES]?.let(::parseMap) ?: emptyMap(),
            areas = this[KEY_AREAS]?.let(::parseMap) ?: emptyMap(),
            customAreas = customAreas(),
            salary = when {
                this[KEY_SALARY] == SALARY_NONE -> SalaryChoice.None
                this[KEY_SALARIES].isNullOrEmpty() -> SalaryChoice.Automatic
                else -> SalaryChoice.Fixed(this[KEY_SALARIES]!!)
            },
            rules = this[KEY_RULES]?.let(::parseRules) ?: emptyList(),
            rulesMigrated = this[KEY_RULES_MIGRATED] ?: false
        )

        fun Preferences.news() = this[KEY_NEWS]?.let(::parseNews) ?: emptyList()

        fun Preferences.customAreas() = this[KEY_CUSTOM_AREAS]?.let(::parseCustomAreas) ?: emptyList()
    }
}
