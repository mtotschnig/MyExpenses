package org.totschnig.myexpenses.next.data.contracts


/**
 * Decisions of the user about detected contracts.
 *
 * @param dismissed signatures of contracts the user removed as "not a contract"
 * @param names custom names by signature
 * @param areas keys of the categories chosen by the user (or [AREA_NONE]) by signature,
 * contracts without entry use the suggested category
 * @param customAreas categories created by the user, in the order they were created
 * @param salary which regular incomes are salaries
 * @param rules confirmed contracts and payments that are no contract, see [ContractRule]
 * @param rulesMigrated whether [dismissed], [names] and [areas], which were stored by signature
 * before there were rules, have been taken over into [rules]
 */
data class ContractSettings(
    val dismissed: Set<String> = emptySet(),
    val names: Map<String, String> = emptyMap(),
    val areas: Map<String, String> = emptyMap(),
    val customAreas: List<CustomArea> = emptyList(),
    val salary: SalaryChoice = SalaryChoice.Automatic,
    val rules: List<ContractRule> = emptyList(),
    val rulesMigrated: Boolean = false,
) {
    fun areaChoice(signature: String) = areaChoiceOf(areas[signature])

    /**
     * @param key of a [ContractArea], [AREA_NONE], or null for the suggested category
     */
    fun areaChoiceOf(key: String?): AreaChoice {
        if (key == null) return AreaChoice.Automatic
        if (key == AREA_NONE) return AreaChoice.Fixed(null)
        // A deleted custom category falls back to the suggestion
        val area = BuiltInArea.fromKey(key) ?: customAreas.find { it.key == key } ?: return AreaChoice.Automatic
        return AreaChoice.Fixed(area)
    }

    companion object {
        /** Stored for contracts the user explicitly put into no category */
        const val AREA_NONE = "NONE"
    }
}

/**
 * Contracts or incomes found in the analysed transactions, shown as
 * [org.totschnig.myexpenses.next.ledger.AnalysisState]
 */
data class ContractsOverview(
    /** Confirmed, sorted by monthly amount, highest first */
    val active: List<Contract>,
    /** Confirmed, not paid anymore without being cancelled, last debited first */
    val ended: List<Contract>,
    /** Removed by the user, sorted by name */
    val dismissed: List<Contract>,
    /**
     * Active contracts detected, but not confirmed by the user, sorted by monthly amount.
     * They do not count in any sums.
     */
    val suggestions: List<Contract> = emptyList(),
    /** Marked as cancelled by the user, last cancelled first. They do not count in any sums. */
    val cancelled: List<Contract> = emptyList(),
    val customAreas: List<CustomArea> = emptyList(),
    val salaryChoice: SalaryChoice = SalaryChoice.Automatic,
) {
    /** The regular incomes that are salaries, highest first, see [SalaryChoice] */
    val salaries: List<Contract> get() = active.salaries(salaryChoice)

    fun isSalary(contract: Contract) = salaries.any { it.signature == contract.signature }

    val isEmpty: Boolean
        get() = active.isEmpty() && ended.isEmpty() && dismissed.isEmpty() && suggestions.isEmpty() &&
                cancelled.isEmpty()

    /**
     * Categories that get a tab: built-in ones only if they contain contracts,
     * custom ones always, so that a new category can be filled.
     */
    val areas: List<ContractArea>
        get() = BuiltInArea.entries.filter { area -> (active + ended).any { it.area == area } } + customAreas

    /** All categories a contract can be put into */
    val selectableAreas: List<ContractArea> get() = BuiltInArea.entries + customAreas

    /** Only contracts of [area]. Suggestions are decided on in the list of all contracts. */
    fun forArea(area: ContractArea) = copy(
        active = active.filter { it.area == area },
        ended = ended.filter { it.area == area },
        dismissed = dismissed.filter { it.area == area },
        suggestions = emptyList(),
        cancelled = cancelled.filter { it.area == area }
    )
}

/**
 * Applies the [settings] to the [contracts] of a [ContractAnalysis]: names and categories from the
 * rules (or, before they were taken over, from the maps by signature). Separates suggestions from
 * confirmed contracts.
 *
 * @param dismissed payments the user declared as no contract, see [ContractAnalysis.dismissed]
 */
fun buildContractsState(
    contracts: List<Contract>,
    settings: ContractSettings,
    dismissed: List<Contract> = emptyList(),
): ContractsOverview {
    val rules = settings.rules.associateBy { it.key }
    fun Contract.withDecisions() = rules[signature]?.let { rule ->
        copy(customName = rule.name, areaChoice = settings.areaChoiceOf(rule.areaKey))
    } ?: copy(customName = settings.names[signature], areaChoice = settings.areaChoice(signature))

    val (dismissedBefore, shown) = contracts
        .map { it.withDecisions() }
        .partition { it.signature in settings.dismissed }
    val (confirmed, detected) = shown.partition { it.isConfirmed }
    val (cancelled, running) = confirmed.partition { it.isCancelled }
    val (active, ended) = running.partition { it.isActive }
    return ContractsOverview(
        // A suggestion that is not paid anymore is not worth a decision
        suggestions = detected.filter { it.isActive }.sortedByDescending { it.monthlyAmount },
        active = active.sortedByDescending { it.monthlyAmount },
        cancelled = cancelled.sortedByDescending { it.cancelledOn },
        ended = ended.sortedByDescending { it.lastDate },
        dismissed = (dismissedBefore + dismissed.map { it.withDecisions() }).sortedBy { it.displayName.lowercase() },
        customAreas = settings.customAreas,
        salaryChoice = settings.salary
    )
}
