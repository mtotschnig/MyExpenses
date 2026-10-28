package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate

/**
 * What is stored after an analysis without asking the user, see [automaticDecisions]
 *
 * @param rules all rules, with the suggestions that are certain enough confirmed and cancellations
 * lifted for contracts that were paid again
 * @param news all news, see [ContractNews]
 * @param migratedSalary the salary taken over from the decisions stored by signature before there
 * were rules; null if they were not taken over now
 * @param marksMigration the decisions stored by signature are dealt with from now on, see
 * [ContractSettings.rulesMigrated]
 */
class AutomaticDecisions(
    val rules: List<ContractRule>,
    val news: List<ContractNews>,
    val migratedSalary: SalaryChoice? = null,
    val marksMigration: Boolean = false,
)

/**
 * Once: takes over the decisions stored by signature before there were rules. Then: confirms
 * suggestions that are certain enough, adds news and lifts the cancellation of contracts that were
 * paid again.
 *
 * @param storedNews the news stored so far
 */
fun automaticDecisions(
    settings: ContractSettings,
    storedNews: List<ContractNews>,
    analysis: ContractAnalysis,
    today: LocalDate,
): AutomaticDecisions {
    var rules = settings.rules
    var migratedSalary: SalaryChoice? = null
    // Only an analysis without rules shows the contracts the old decisions refer to
    if (!settings.rulesMigrated && rules.isEmpty()) {
        val (migrated, salary) = migrateToRules(settings, analysis)
        rules = migrated
        migratedSalary = salary
    }
    val detection = analysis.detectNews(today)
    val confirmations = analysis.automaticConfirmations(rules)
    // The first analysis confirms all contracts found so far, which are nothing new to the user
    val newContracts = if (settings.rulesMigrated) newContractNews(confirmations, today) else emptyList()
    return AutomaticDecisions(
        rules = (rules + confirmations.map { it.second }).reactivating(detection.reactivated),
        news = storedNews.settling(analysis).adding(detection.news + newContracts, today),
        migratedSalary = migratedSalary,
        marksMigration = !settings.rulesMigrated
    )
}
