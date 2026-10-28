package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate

/**
 * Result of applying the rules of the user to the analysed transactions
 *
 * @param confirmed contracts defined by rules of kind [ContractRule.Kind.CONTRACT]
 * @param ignored payments the user declared as no contract, by id of the rule
 * @param remaining payments no rule covers, left for [ContractDetector] to find suggestions in
 */
class RuleMatch(
    val confirmed: List<Contract>,
    val ignored: Map<String, List<ContractTransaction>>,
    val remaining: List<ContractTransaction>,
)

/**
 * Applies the rules of the user to transactions. Each payment belongs to at most one rule:
 * contracts before ignored payments, and rules with an amount range before those without,
 * since they are more specific.
 */
class RuleMatcher(private val today: LocalDate = LocalDate.now()) {

    fun apply(rules: List<ContractRule>, transactions: List<ContractTransaction>): RuleMatch {
        val ordered = rules.sortedWith(
            compareBy({ it.kind != ContractRule.Kind.CONTRACT }, { it.amountRange == null })
        )
        val matched = LinkedHashMap<ContractRule, MutableList<ContractTransaction>>()
        val remaining = mutableListOf<ContractTransaction>()
        transactions
            .filter { it.amount != 0L }
            .sortedWith(compareBy({ it.date }, { it.id }))
            .forEach { transaction ->
                val rule = ordered.firstOrNull { it.matches(transaction) }
                if (rule == null) remaining += transaction
                else matched.getOrPut(rule) { mutableListOf() } += transaction
            }
        // Cancelled contracts stay visible, also when their payments are older than the analysed period
        val onlyKnownFromSnapshot = rules.filter {
            it.kind == ContractRule.Kind.CONTRACT && it.snapshot != null && it !in matched
        }
        return RuleMatch(
            confirmed = matched
                .filterKeys { it.kind == ContractRule.Kind.CONTRACT }
                .map { (rule, payments) -> rule.toContract(payments) } +
                    onlyKnownFromSnapshot.map { rule -> rule.toContract(listOf(rule.snapshotPayment())) },
            ignored = matched
                .filterKeys { it.kind == ContractRule.Kind.IGNORE }
                .mapKeys { it.key.id },
            remaining = remaining
        )
    }

    /**
     * The category of the rule is applied with the settings, since custom categories are needed for it
     */
    private fun ContractRule.toContract(payments: List<ContractTransaction>) =
        contractOf(key, payments, interval, direction, today, isConfirmed = true).let {
            it.copy(
                customName = name,
                rule = this,
                cancelledOn = cancelledOn,
                reserveChoice = reserve,
                // Not expected to be paid anymore
                isActive = it.isActive && cancelledOn == null
            )
        }

    private fun ContractRule.snapshotPayment() = snapshot!!.let {
        ContractTransaction(
            id = SNAPSHOT_TRANSACTION_ID,
            date = it.lastDate,
            amount = if (direction == ContractDirection.INCOME) it.lastAmount else -it.lastAmount,
            accountId = 0,
            payeeName = it.name
        )
    }
}
