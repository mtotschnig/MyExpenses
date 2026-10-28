package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate

/**
 * Contracts and regular incomes found in [transactions]: confirmed ones from the rules of the user,
 * suggestions from [ContractDetector] for the payments no rule covers, and the payments the user
 * declared as no contract.
 *
 * @param contracts confirmed contracts and suggestions
 * @param dismissed payments of rules of kind [ContractRule.Kind.IGNORE], one entry per rule
 * @param today the day it was made for
 */
class ContractAnalysis(
    val transactions: List<ContractTransaction>,
    val contracts: List<Contract>,
    val dismissed: List<Contract>,
    val today: LocalDate,
) {
    val suggestions: List<Contract> get() = contracts.filter { !it.isConfirmed }

    companion object {
        fun of(transactions: List<ContractTransaction>, rules: List<ContractRule>, today: LocalDate): ContractAnalysis {
            val match = RuleMatcher(today).apply(rules, transactions)
            val rulesById = rules.associateBy { it.id }
            return ContractAnalysis(
                transactions = transactions,
                contracts = match.confirmed + ContractDetector(today).detect(match.remaining),
                dismissed = match.ignored.map { (id, payments) ->
                    val rule = rulesById.getValue(id)
                    contractOf(rule.key, payments, rule.interval, rule.direction, today)
                        .copy(customName = rule.name)
                },
                today = today
            )
        }
    }
}

/**
 * Rules for the suggestions that are certain enough to be confirmed without asking the user.
 * Only active ones: the payee of an ended contract may later be paid for other things.
 *
 * @param existing rules already stored: suggestions with payments they cover get no rule, so that
 * nothing is added twice, e.g. when the analysis ran again before the stored rules arrived
 */
fun ContractAnalysis.automaticRules(existing: List<ContractRule>): List<ContractRule> =
    automaticConfirmations(existing).map { it.second }

/**
 * Like [automaticRules], with the suggestion each rule confirms
 */
fun ContractAnalysis.automaticConfirmations(existing: List<ContractRule>): List<Pair<Contract, ContractRule>> =
    suggestions
        .filter { it.isConfident && it.isActive }
        .filter { contract -> contract.transactions.none { payment -> existing.any { it.matches(payment) } } }
        .map { it to ContractRule.of(it, transactions) }

/**
 * Takes over the decisions the user made before there were rules: they were stored by the
 * signature of a detected contract, now they belong to a rule.
 *
 * @param analysis of all transactions without any rules
 * @return the rules, and the salary choice with signatures replaced by the keys of the rules
 */
fun migrateToRules(settings: ContractSettings, analysis: ContractAnalysis): Pair<List<ContractRule>, SalaryChoice> {
    val salarySignatures = (settings.salary as? SalaryChoice.Fixed)?.signatures.orEmpty()
    val keys = mutableMapOf<String, String>()
    val rules = analysis.contracts.mapNotNull { contract ->
        val signature = contract.signature
        val kind = when {
            signature in settings.dismissed -> ContractRule.Kind.IGNORE
            signature in settings.names || signature in settings.areas || signature in salarySignatures ->
                ContractRule.Kind.CONTRACT

            else -> return@mapNotNull null
        }
        ContractRule.of(
            contract.copy(customName = settings.names[signature], areaChoice = settings.areaChoice(signature)),
            analysis.transactions,
            kind
        ).also { keys[signature] = it.key }
    }
    val salary = when (val choice = settings.salary) {
        is SalaryChoice.Fixed -> choice.signatures.mapNotNull { keys[it] }.toSet()
            .let { if (it.isEmpty()) SalaryChoice.Automatic else SalaryChoice.Fixed(it) }

        else -> choice
    }
    return rules to salary
}

/**
 * Applies [change] to the rule of [contract], creating one for a suggestion, which confirms it
 *
 * @return all rules, and the rule of the contract
 */
fun List<ContractRule>.withRuleFor(
    contract: Contract,
    transactions: List<ContractTransaction>,
    change: (ContractRule) -> ContractRule = { it },
): Pair<List<ContractRule>, ContractRule> {
    val existing = find { it.key == contract.signature }
    return if (existing != null) {
        val changed = change(existing)
        map { if (it === existing) changed else it } to changed
    } else {
        val created = change(ContractRule.of(contract, transactions))
        this + created to created
    }
}

/**
 * The user declares [contract] as no contract: its rule, if confirmed, turns into one that ignores its payments
 */
fun List<ContractRule>.dismissing(contract: Contract, transactions: List<ContractTransaction>): List<ContractRule> {
    val existing = find { it.key == contract.signature }
    return if (existing != null) map { if (it === existing) it.copy(kind = ContractRule.Kind.IGNORE) else it }
    else this + ContractRule.of(contract, transactions, ContractRule.Kind.IGNORE)
}

/**
 * The user takes back that the payments of [contract] (from [ContractAnalysis.dismissed]) are no contract,
 * which says they are one: the rule is confirmed, keeping name and category
 */
fun List<ContractRule>.restoring(contract: Contract): List<ContractRule> =
    map { if (it.key == contract.signature) it.copy(kind = ContractRule.Kind.CONTRACT) else it }

/**
 * Joins [other] into [target], e.g. after the provider of a contract changed its name: the rule of
 * [target] gets the payees of [other] and keeps its name, category and interval. The rule of
 * [other], if confirmed, is removed.
 *
 * @return all rules, and the rule of the joined contract
 */
fun List<ContractRule>.merging(
    target: Contract,
    other: Contract,
    transactions: List<ContractTransaction>,
): Pair<List<ContractRule>, ContractRule> {
    val otherRule = find { it.key == other.signature } ?: ContractRule.of(other, transactions)
    return filterNot { it.key == other.signature }.withRuleFor(target, transactions) { rule ->
        rule.copy(
            // Payments of a second template can only be matched through their payees
            payeeIds = rule.payeeIds + otherRule.payeeIds +
                    if (otherRule.templateId != null && otherRule.templateId != rule.templateId)
                        other.transactions.mapNotNull { it.payeeId } else emptyList(),
            templateId = rule.templateId ?: otherRule.templateId,
            // Joining payments to a savings account with the movements to it keeps both
            targetAccountIds = rule.targetAccountIds + otherRule.targetAccountIds,
            // Only if both contracts were limited to amounts, the joined one stays so
            amountRange = rule.amountRange?.let { range ->
                otherRule.amountRange?.let { minOf(range.first, it.first)..maxOf(range.last, it.last) }
            }
        )
    }
}

/**
 * What the analysis knows about a single payment, e.g. for the details of a transaction
 */
sealed interface PaymentContractInfo {
    /** The analysis is still running */
    data object Loading : PaymentContractInfo

    /** Not analysed: no consent, a transfer, older than the analysed period or of an excluded account */
    data object Unavailable : PaymentContractInfo

    /** Without payee (and template), a payment cannot be told apart from others */
    data object NoPayee : PaymentContractInfo

    data class Confirmed(val contract: Contract) : PaymentContractInfo

    data class Suggested(val contract: Contract) : PaymentContractInfo

    data class Dismissed(val contract: Contract) : PaymentContractInfo

    /** Not part of any contract yet, the user can declare it as one */
    data class Markable(val isIncome: Boolean) : PaymentContractInfo
}

fun ContractAnalysis.infoFor(transactionId: Long): PaymentContractInfo {
    val payment = transactions.find { it.id == transactionId } ?: return PaymentContractInfo.Unavailable
    contracts.find { contract -> contract.transactions.any { it.id == transactionId } }?.let {
        return if (it.isConfirmed) PaymentContractInfo.Confirmed(it) else PaymentContractInfo.Suggested(it)
    }
    dismissed.find { contract -> contract.transactions.any { it.id == transactionId } }?.let {
        return PaymentContractInfo.Dismissed(it)
    }
    if (payment.payeeId == null && payment.templateId == null && payment.targetAccountId == null) {
        return PaymentContractInfo.NoPayee
    }
    return PaymentContractInfo.Markable(ContractDirection.of(payment.amount) == ContractDirection.INCOME)
}

/**
 * The user declares [transactionId] as a payment of a contract paid every [interval]: a rule for
 * its payee (or template), limited to similar amounts if the payee is also paid for other things
 */
fun List<ContractRule>.marking(
    transactionId: Long,
    interval: ContractInterval,
    transactions: List<ContractTransaction>,
    today: LocalDate,
): List<ContractRule> {
    val payment = transactions.find { it.id == transactionId } ?: return this
    val contract = contractOf("", listOf(payment), interval, ContractDirection.of(payment.amount), today)
    return this + ContractRule.of(contract, transactions)
}
