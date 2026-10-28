package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * Finds recurring debits (contracts, subscriptions, …) and recurring credits (salary, …)
 * in a list of transactions.
 *
 * 1. Payments are grouped by direction and by the template they were created from, otherwise
 *    by payee. Payments without either are ignored.
 * 2. If the whole group is debited regularly with similar amounts (price changes allowed),
 *    it is one contract. Otherwise the group is split into clusters of similar amounts
 *    (e.g. two different subscriptions from the same payee). Clusters that follow each other
 *    in the rhythm of the series are joined again, since they are one contract with a larger
 *    change of the amount, e.g. a raise of the salary. Each resulting series is checked.
 * 3. A series is regular, if the median of the days between its debits matches a
 *    [ContractInterval] and at least [MIN_REGULAR_SHARE] of all gaps match that interval.
 */
class ContractDetector(private val today: LocalDate = LocalDate.now()) {

    /**
     * Debits and credits are analysed separately, so that e.g. a refund never joins the series
     * of debits of the same payee.
     */
    fun detect(transactions: List<ContractTransaction>): List<Contract> =
        transactions
            .filter { it.amount != 0L }
            .mapNotNull { transaction -> transaction.groupKey()?.let { it to transaction } }
            .groupBy({ it.first }, { it.second })
            .flatMap { (key, group) ->
                val sorted = group.sortedWith(compareBy({ it.date }, { it.id }))
                detectInGroup(key, sorted)
            }
            .sortedByDescending { it.monthlyAmount }

    private data class GroupKey(
        val direction: ContractDirection,
        val isTemplate: Boolean,
        val id: Long,
        val isTargetAccount: Boolean = false,
    ) {
        /**
         * Contracts keep the signatures they had before credits were analysed,
         * so that the stored decisions of the user still apply
         */
        override fun toString() = (if (direction == ContractDirection.INCOME) INCOME_PREFIX else "") +
                (when {
                    isTargetAccount -> TARGET_ACCOUNT_PREFIX
                    isTemplate -> TEMPLATE_PREFIX
                    else -> PAYEE_PREFIX
                }) + id
    }

    /**
     * Movements between own accounts are grouped by the account the money goes to, since the payee
     * is the user or missing
     */
    private fun ContractTransaction.groupKey(): GroupKey? {
        val direction = ContractDirection.of(amount)
        return targetAccountId?.let { GroupKey(direction, false, it, isTargetAccount = true) }
            ?: templateId?.let { GroupKey(direction, true, it) }
            ?: payeeId?.let { GroupKey(direction, false, it) }
    }

    private fun detectInGroup(
        groupKey: GroupKey,
        group: List<ContractTransaction>,
    ): List<Contract> {
        val key = groupKey.toString()
        val isTemplate = groupKey.isTemplate
        // Transactions created from a template belong together, whatever their amount
        val series = if (isTemplate || group.hasSimilarAmounts(GROUP_AMOUNT_TOLERANCE)) {
            detectSeries(group, groupKey.direction)?.let { listOf(it) }
        } else null
        return (series ?: if (isTemplate) emptyList() else
            group.clusterByAmount(CLUSTER_AMOUNT_TOLERANCE).let { clusters ->
                // Only a payee with a clear rhythm, e.g. an employer, continues a series with other
                // amounts. For shops, joining single purchases would invent contracts.
                val interval = group.usualInterval()
                if (interval == null) clusters
                else clusters.flatMap { cluster ->
                    // A cluster with a rhythm of its own is a separate contract, e.g. a yearly one
                    val own = regularInterval(cluster)
                    if (own != null && own != interval) listOf(cluster) else cluster.splitAtGaps(interval)
                }.joinConsecutive(interval)
            }.mapNotNull { detectSeries(it, groupKey.direction) })
            .withSignatures(key, groupKey.direction, isTemplate)
    }

    private class Series(val interval: ContractInterval, val transactions: List<ContractTransaction>)

    /**
     * @param transactions sorted by date
     */
    private fun detectSeries(transactions: List<ContractTransaction>, direction: ContractDirection): Series? {
        val interval = regularInterval(transactions) ?: return null
        if (transactions.size < interval.minOccurrences) return null
        if (direction == ContractDirection.INCOME && !transactions.isLikelyRareIncome(interval)) return null
        return Series(interval, transactions)
    }

    /**
     * Yearly and half-yearly incomes are received only twice in the analysed period, and a single
     * gap can match by chance, e.g. with gifts, tax refunds or credits from annual statements.
     * With only two payments, they must therefore come on the expected day and with the same amount.
     */
    private fun List<ContractTransaction>.isLikelyRareIncome(interval: ContractInterval): Boolean {
        if (interval != ContractInterval.YEARLY && interval != ContractInterval.HALF_YEARLY) return true
        if (size > 2) return true
        val (first, second) = this
        val daysOff = ChronoUnit.DAYS.between(first.date.plus(interval.step), second.date).absoluteValue
        val amounts = listOf(first.amount.absoluteValue, second.amount.absoluteValue)
        return daysOff <= RARE_INCOME_MAX_DAYS_OFF &&
                amounts.max() <= amounts.min() * (1 + RARE_INCOME_AMOUNT_TOLERANCE)
    }

    /**
     * The interval, if [transactions] (sorted by date) follow one regularly, regardless of how many they are
     */
    private fun regularInterval(transactions: List<ContractTransaction>): ContractInterval? {
        if (transactions.size < 2) return null
        val gaps = transactions.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date) }
        val median = gaps.sorted()[gaps.size / 2]
        val interval = ContractInterval.forDays(median) ?: return null
        return interval.takeIf { gaps.count { it in interval } >= gaps.size * MIN_REGULAR_SHARE }
    }

    /**
     * The interval payments of a whole group (sorted by date) usually follow, even if it is
     * not regular as a whole, e.g. because of bonus payments in between
     */
    private fun List<ContractTransaction>.usualInterval(): ContractInterval? {
        if (size < 2) return null
        val gaps = zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date) }
        return ContractInterval.forDays(gaps.sorted()[gaps.size / 2])
    }

    /**
     * Splits a cluster of similar amounts (sorted by date) where it pauses longer than [interval]
     * of the whole group. With an amount that varies a lot, e.g. a salary, amounts of distant
     * months then do not end up in one cluster, which would hide that the series continued
     * with other amounts.
     */
    private fun List<ContractTransaction>.splitAtGaps(interval: ContractInterval): List<List<ContractTransaction>> {
        val runs = mutableListOf(mutableListOf(first()))
        zipWithNext().forEach { (previous, next) ->
            if (ChronoUnit.DAYS.between(previous.date, next.date) > interval.maxDays) runs += mutableListOf<ContractTransaction>()
            runs.last() += next
        }
        return runs
    }

    /**
     * Joins runs of amounts that follow each other: a run starts after the last payment of
     * another one, at most one missed payment later, and together they stay regular. That is a
     * change of the amount, e.g. a raise, not a second contract, which would run at the same time.
     *
     * @param this runs, each sorted by date
     */
    private fun List<List<ContractTransaction>>.joinConsecutive(interval: ContractInterval): List<List<ContractTransaction>> {
        val series = mutableListOf<List<ContractTransaction>>()
        sortedBy { it.first().date }.forEach { cluster ->
            val predecessor = series.indices
                .filter { series[it].isContinuedBy(cluster, interval) }
                .maxByOrNull { series[it].last().date }
            if (predecessor != null) series[predecessor] = series[predecessor] + cluster
            else series += cluster
        }
        return series
    }

    private fun List<ContractTransaction>.isContinuedBy(next: List<ContractTransaction>, interval: ContractInterval): Boolean {
        if (!next.first().date.isAfter(last().date)) return false
        if (regularInterval(this + next) != interval) return false
        val gap = ChronoUnit.DAYS.between(last().date, next.first().date)
        return gap >= interval.minDays && gap <= interval.maxDays * 2
    }

    /**
     * The signature is made of group key and interval. In the rare case that a payee has several
     * contracts with the same interval, they are told apart by the order of their amounts.
     */
    private fun List<Series>.withSignatures(key: String, direction: ContractDirection, isTemplate: Boolean) =
        groupBy { it.interval }.flatMap { (interval, sameInterval) ->
            sameInterval.sortedBy { series -> series.transactions.minOf { it.amount.absoluteValue } }
                .mapIndexed { index, series ->
                    toContract(
                        signature = "$key|${interval.name}" + if (index > 0) "|$index" else "",
                        series = series,
                        direction = direction,
                        isTemplate = isTemplate
                    )
                }
        }

    private fun toContract(signature: String, series: Series, direction: ContractDirection, isTemplate: Boolean) =
        contractOf(signature, series.transactions, series.interval, direction, today)
            .copy(isConfident = isTemplate || series.isRegularEnoughToConfirm())

    /**
     * Many payments that nearly all follow the interval: a contract without doubt
     */
    private fun Series.isRegularEnoughToConfirm(): Boolean {
        if (transactions.size < CONFIDENT_MIN_PAYMENTS) return false
        val gaps = transactions.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.date, b.date) }
        return gaps.count { it in interval } >= gaps.size * CONFIDENT_REGULAR_SHARE
    }

    private fun List<ContractTransaction>.hasSimilarAmounts(tolerance: Double): Boolean {
        val amounts = map { it.amount.absoluteValue }.sorted()
        val median = amounts[amounts.size / 2]
        return amounts.first() >= median * (1 - tolerance) && amounts.last() <= median * (1 + tolerance)
    }

    /**
     * Splits into clusters of similar amounts, each sorted by date.
     */
    private fun List<ContractTransaction>.clusterByAmount(tolerance: Double): List<List<ContractTransaction>> {
        val clusters = mutableListOf<MutableList<ContractTransaction>>()
        var clusterStart = 0L
        sortedBy { it.amount.absoluteValue }.forEach {
            val amount = it.amount.absoluteValue
            if (clusters.isEmpty() || amount > clusterStart * (1 + tolerance)) {
                clusters.add(mutableListOf())
                clusterStart = amount
            }
            clusters.last().add(it)
        }
        return clusters.map { cluster -> cluster.sortedWith(compareBy({ it.date }, { it.id })) }
    }

    companion object {
        private const val TEMPLATE_PREFIX = "t"
        private const val PAYEE_PREFIX = "p"
        private const val TARGET_ACCOUNT_PREFIX = "a"
        private const val INCOME_PREFIX = "in:"

        /** Share of gaps between debits that must match the detected interval */
        const val MIN_REGULAR_SHARE = 0.75

        /** Maximum deviation from the median amount for a whole payee to count as one contract */
        const val GROUP_AMOUNT_TOLERANCE = 0.35

        /** Maximum deviation of amounts within one cluster */
        const val CLUSTER_AMOUNT_TOLERANCE = 0.2

        /** How many days a yearly or half-yearly income with only two payments may deviate from the expected date */
        const val RARE_INCOME_MAX_DAYS_OFF = 7

        /** How much the two payments of a yearly or half-yearly income may differ */
        const val RARE_INCOME_AMOUNT_TOLERANCE = 0.1

        /** A suggestion with at least this many payments … */
        const val CONFIDENT_MIN_PAYMENTS = 6

        /** … whose gaps match the interval at least to this share is confirmed without asking the user */
        const val CONFIDENT_REGULAR_SHARE = 0.9

        /** A contract counts as ended, if it has not been debited for its interval plus this grace period */
        const val MIN_GRACE_DAYS = 7
    }
}
