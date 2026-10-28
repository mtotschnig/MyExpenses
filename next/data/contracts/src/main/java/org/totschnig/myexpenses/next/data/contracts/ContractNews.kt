package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * Something that happened with a confirmed contract (or regular income) that the user should know
 * about. News are stored when they happen, so that they stay in the history even when the situation
 * changes afterwards.
 *
 * @param id made of [type], rule and payment, so that the same news is never stored twice
 * @param ruleId the [ContractRule] of the contract
 * @param transactionId the payment that caused the news, for news about missing payments the last one
 * @param date of that payment, for [Type.PAYMENT_MISSING] the date the payment was expected
 * @param createdAt when the news was detected
 * @param contractName name of the contract when the news was detected, for the history
 * @param amount of the payment, absolute, in minor units of the home currency
 * @param previousAmount for [Type.PRICE_CHANGE]: the amount before
 * @param cancelledOn for [Type.PAYMENT_AFTER_CANCELLATION]: when the contract had been cancelled
 * @param interval for [Type.NEW_CONTRACT]: how often the new contract is paid
 */
data class ContractNews(
    val id: String,
    val type: Type,
    val ruleId: String,
    val transactionId: Long,
    val date: LocalDate,
    val createdAt: LocalDate,
    val contractName: String,
    val isIncome: Boolean,
    /** The contract puts money aside, which changes the wording, see [Reserve] */
    val isReserve: Boolean = false,
    val amount: Long,
    val previousAmount: Long? = null,
    val cancelledOn: LocalDate? = null,
    val interval: ContractInterval? = null,
    val isRead: Boolean = false,
) {
    enum class Type {
        /** A cancelled contract was paid again, which lifted the cancellation */
        PAYMENT_AFTER_CANCELLATION,

        /** The amount changed after it had been stable */
        PRICE_CHANGE,

        /** A suggestion was confirmed automatically */
        NEW_CONTRACT,

        /** An active contract has not been paid when expected */
        PAYMENT_MISSING,

        /** A confirmed contract that was not cancelled is not paid anymore */
        CONTRACT_STOPPED
    }

    /** Key of the contract, see [ContractRule.key] */
    val contractKey: String get() = "${ContractRule.KEY_PREFIX}$ruleId"

    companion object {
        /** A change of the amount below this share is no price change, e.g. rounding of exchange rates */
        const val PRICE_CHANGE_TOLERANCE = 0.01

        /** Only this recent events are reported, not the history found on the first analysis */
        const val MAX_AGE_DAYS = 40L

        /** A payment is reported as missing not before this many days after it was expected */
        const val MISSING_MIN_DAYS = 3L

        /** How long news are kept in the history */
        const val KEEP_MONTHS = 24L

        /** At most this many news are kept */
        const val MAX_COUNT = 200
    }
}

/**
 * @param news found in the analysis, including those stored already
 * @param reactivated rules of cancelled contracts that were paid again
 */
class NewsDetection(val news: List<ContractNews>, val reactivated: Set<String>)

/**
 * Finds news in the confirmed contracts of [analysis]. Payments dated in the future, e.g. booked
 * in advance by a plan, have not happened yet and are left out.
 */
fun ContractAnalysis.detectNews(today: LocalDate): NewsDetection {
    val news = mutableListOf<ContractNews>()
    val reactivated = mutableSetOf<String>()
    contracts.filter { it.isConfirmed }.forEach { contract ->
        val rule = contract.rule ?: return@forEach
        val payments = contract.transactions.filter { it.id != SNAPSHOT_TRANSACTION_ID && !it.date.isAfter(today) }
        fun newsOf(type: ContractNews.Type, payment: ContractTransaction, previousAmount: Long? = null) = ContractNews(
            id = "${type.name}:${rule.id}:${payment.id}",
            type = type,
            ruleId = rule.id,
            transactionId = payment.id,
            date = payment.date,
            createdAt = today,
            contractName = contract.displayName,
            isIncome = contract.isIncome,
            isReserve = contract.isReserve,
            amount = payment.amount.absoluteValue,
            previousAmount = previousAmount,
            cancelledOn = rule.cancelledOn
        )

        val cancelledOn = rule.cancelledOn
        if (cancelledOn != null) {
            payments.lastOrNull { it.date.isAfter(cancelledOn) }?.let {
                news += newsOf(ContractNews.Type.PAYMENT_AFTER_CANCELLATION, it)
                reactivated += rule.id
            }
        }

        if (payments.size >= 3) {
            val (beforePrevious, previous, last) = payments.takeLast(3).map { it.amount.absoluteValue }
            fun differ(a: Long, b: Long) = (a - b).absoluteValue > b * ContractNews.PRICE_CHANGE_TOLERANCE
            val lastPayment = payments.last()
            if (!differ(previous, beforePrevious) && differ(last, previous) &&
                ChronoUnit.DAYS.between(lastPayment.date, today) <= ContractNews.MAX_AGE_DAYS
            ) {
                news += newsOf(ContractNews.Type.PRICE_CHANGE, lastPayment, previousAmount = previous)
            }
        }

        val lastPayment = payments.lastOrNull() ?: return@forEach
        if (contract.isActive) {
            val expected = contract.nextExpectedDate
            if (today.isAfter(expected.plusDays(contract.interval.missingTolerance()))) {
                news += newsOf(ContractNews.Type.PAYMENT_MISSING, lastPayment).let {
                    // One news per expected payment
                    it.copy(id = "${it.type.name}:${rule.id}:$expected", date = expected)
                }
            }
        } else if (!contract.isCancelled) {
            val endedOn = lastPayment.date.plusDays((contract.interval.maxDays + contract.interval.graceDays()).toLong())
            if (!endedOn.isAfter(today) && ChronoUnit.DAYS.between(endedOn, today) <= ContractNews.MAX_AGE_DAYS) {
                news += newsOf(ContractNews.Type.CONTRACT_STOPPED, lastPayment)
            }
        }
    }
    return NewsDetection(news, reactivated)
}

/**
 * How late a payment may be before it is missing: the spread of the interval beyond its step,
 * e.g. 5 days for monthly payments, which are moved by weekends and bank holidays
 */
private fun ContractInterval.missingTolerance(): Long {
    val stepDays = ChronoUnit.DAYS.between(LocalDate.of(2001, 1, 1), LocalDate.of(2001, 1, 1).plus(step))
    return maxOf(ContractNews.MISSING_MIN_DAYS, maxDays - stepDays)
}

/**
 * News for suggestions that were confirmed automatically
 */
fun newContractNews(confirmed: List<Pair<Contract, ContractRule>>, today: LocalDate): List<ContractNews> =
    confirmed.map { (contract, rule) ->
        val last = contract.lastTransaction
        ContractNews(
            id = "${ContractNews.Type.NEW_CONTRACT.name}:${rule.id}:${last.id}",
            type = ContractNews.Type.NEW_CONTRACT,
            ruleId = rule.id,
            transactionId = last.id,
            date = last.date,
            createdAt = today,
            contractName = contract.displayName,
            isIncome = contract.isIncome,
            isReserve = contract.isReserve,
            amount = contract.lastAmount,
            interval = contract.interval
        )
    }

/**
 * Marks news as read that are settled: a missing payment arrived, a stopped contract was cancelled
 * or is paid again
 */
fun List<ContractNews>.settling(analysis: ContractAnalysis): List<ContractNews> {
    val contracts = analysis.contracts.associateBy { it.signature }
    return map { news ->
        if (news.isRead) return@map news
        val contract = contracts[news.contractKey]
        val settled = when (news.type) {
            ContractNews.Type.PAYMENT_MISSING -> contract == null || contract.nextExpectedDate.isAfter(news.date)
            ContractNews.Type.CONTRACT_STOPPED -> contract == null || contract.isActive || contract.isCancelled
            else -> false
        }
        if (settled) news.copy(isRead = true) else news
    }
}

/**
 * Adds the news not stored yet, drops those older than [ContractNews.KEEP_MONTHS] and keeps at
 * most [ContractNews.MAX_COUNT], newest first
 */
fun List<ContractNews>.adding(found: List<ContractNews>, today: LocalDate): List<ContractNews> {
    val known = mapTo(HashSet()) { it.id }
    val limit = today.minusMonths(ContractNews.KEEP_MONTHS)
    return (this + found.filter { it.id !in known })
        .filter { !it.createdAt.isBefore(limit) }
        .sortedWith(compareByDescending<ContractNews> { it.createdAt }.thenByDescending { it.date })
        .take(ContractNews.MAX_COUNT)
}

/**
 * Lifts the cancellation of the rules of contracts that were paid again
 */
fun List<ContractRule>.reactivating(ruleIds: Set<String>): List<ContractRule> =
    if (ruleIds.isEmpty()) this
    else map { if (it.id in ruleIds) it.copy(cancelledOn = null, snapshot = null) else it }
