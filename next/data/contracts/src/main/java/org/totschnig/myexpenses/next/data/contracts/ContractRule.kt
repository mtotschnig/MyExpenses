package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Decision of the user about which payments form a contract (or a regular income), or that they
 * do not. Unlike a suggestion of [ContractDetector], a contract defined by a rule does not depend on
 * heuristics: every matching payment belongs to it, whatever its amount or date.
 *
 * A payment matches, if it goes in [direction], comes from the template [templateId] or from one of
 * [payeeIds], and, if given, its amount lies in [amountRange].
 *
 * @param amountRange absolute amounts in minor units, only for payees that also have payments
 * with other amounts that are not part of the contract, e.g. single orders besides a subscription
 * @param interval how often the contract is paid, as confirmed by the user
 * @param name custom name
 * @param areaKey key of the chosen [ContractArea], [ContractSettings.AREA_NONE] for none,
 * null for the suggested one
 * @param cancelledOn when the user marked the contract as cancelled (for an income: discontinued)
 * @param snapshot the contract when it was cancelled, to still show it when its payments are
 * older than the analysed period
 * @param reserve whether the payments put money aside, null for the suggestion of [Reserve]
 * @param icon chosen by the user, of the [org.totschnig.myexpenses.next.icons.IconCatalog], null
 * for the one of [ContractTile]
 * @param targetAccountIds for movements between own accounts: the accounts the money goes to.
 * Movements are matched only by them, payments to others only by payees or template. A rule can have
 * both, e.g. after the payments to a savings account were joined with the movements to it.
 */
data class ContractRule(
    val id: String,
    val kind: Kind,
    val direction: ContractDirection,
    val payeeIds: Set<Long> = emptySet(),
    val templateId: Long? = null,
    val amountRange: LongRange? = null,
    val interval: ContractInterval,
    val name: String? = null,
    val areaKey: String? = null,
    val cancelledOn: LocalDate? = null,
    val snapshot: Snapshot? = null,
    val reserve: Boolean? = null,
    val targetAccountIds: Set<Long> = emptySet(),
    val icon: String? = null,
) {
    /**
     * @param name detected name of the contract
     * @param lastAmount absolute, in minor units
     */
    data class Snapshot(val name: String, val lastAmount: Long, val lastDate: LocalDate)

    enum class Kind {
        /** The payments form a contract */
        CONTRACT,

        /** The payments are no contract, they are not suggested again */
        IGNORE
    }

    /** Identifies the contract of this rule, e.g. for the salary choice */
    val key: String get() = "$KEY_PREFIX$id"

    fun matches(transaction: ContractTransaction): Boolean =
        ContractDirection.of(transaction.amount) == direction &&
                (transaction.targetAccountId?.let { it in targetAccountIds } ?: (
                        templateId != null && transaction.templateId == templateId ||
                                transaction.payeeId != null && transaction.payeeId in payeeIds)) &&
                (amountRange == null || transaction.amount.absoluteValue in amountRange)

    companion object {
        const val KEY_PREFIX = "rule:"

        /** Margin around the amounts of a contract, when a rule has to tell them apart from other payments */
        const val AMOUNT_RANGE_TOLERANCE = 0.2

        /**
         * Rule for the payments of a detected [contract].
         *
         * @param transactions all analysed transactions, to find out whether the payee also has
         * payments that do not belong to the contract
         */
        fun of(
            contract: Contract,
            transactions: List<ContractTransaction>,
            kind: Kind = Kind.CONTRACT,
            id: String = UUID.randomUUID().toString(),
        ): ContractRule {
            val payments = contract.transactions
            val (movements, others) = payments.partition { it.targetAccountId != null }
            val targetAccountIds = movements.mapNotNullTo(HashSet()) { it.targetAccountId }
            // Payments from a template stay with it, even if the payee is changed
            val templateId = others.map { it.templateId }.distinct().singleOrNull()
            val payeeIds = if (templateId != null) emptySet() else others.mapNotNullTo(HashSet()) { it.payeeId }
            val rule = ContractRule(
                id = id,
                kind = kind,
                direction = contract.direction,
                payeeIds = payeeIds,
                templateId = templateId,
                interval = contract.interval,
                name = contract.customName,
                reserve = contract.reserveChoice,
                targetAccountIds = targetAccountIds,
                areaKey = when (val choice = contract.areaChoice) {
                    AreaChoice.Automatic -> null
                    is AreaChoice.Fixed -> choice.area?.key ?: ContractSettings.AREA_NONE
                }
            )
            val ids = payments.mapTo(HashSet()) { it.id }
            val otherPayments = transactions.filter { it.id !in ids && rule.matches(it) }
            if (otherPayments.isEmpty()) return rule
            val amounts = payments.map { it.amount.absoluteValue }
            val withRange = rule.copy(
                amountRange = (amounts.min() * (1 - AMOUNT_RANGE_TOLERANCE)).roundToLong()..
                        (amounts.max() * (1 + AMOUNT_RANGE_TOLERANCE)).roundToLong()
            )
            // A range that does not tell anything apart would only cut off a future raise
            return if (otherPayments.any { !withRange.matches(it) }) withRange else rule
        }
    }
}
