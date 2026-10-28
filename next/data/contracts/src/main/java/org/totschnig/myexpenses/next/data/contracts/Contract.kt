package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import org.totschnig.myexpenses.next.core.common.NO_NAME

/**
 * How often a contract is debited.
 *
 * @param minDays / [maxDays] range of days between two debits that still counts as this interval.
 * Generous enough to cover debits moved by weekends and bank holidays.
 * @param minOccurrences how many debits are needed at least before a series is accepted as contract
 * @param perYear number of debits per year, used to compute monthly and yearly totals
 * @param step period added to the last debit to get the next expected one
 */
enum class ContractInterval(
    val minDays: Int,
    val maxDays: Int,
    val minOccurrences: Int,
    val perYear: Int,
    val step: Period,
) {
    WEEKLY(6, 8, 4, 52, Period.ofWeeks(1)),
    BIWEEKLY(13, 16, 3, 26, Period.ofWeeks(2)),
    MONTHLY(26, 35, 3, 12, Period.ofMonths(1)),
    BIMONTHLY(55, 67, 3, 6, Period.ofMonths(2)),
    QUARTERLY(82, 100, 3, 4, Period.ofMonths(3)),
    HALF_YEARLY(170, 195, 2, 2, Period.ofMonths(6)),
    YEARLY(345, 385, 2, 1, Period.ofYears(1));

    operator fun contains(days: Long) = days in minDays..maxDays

    companion object {
        fun forDays(days: Long) = entries.firstOrNull { days in it }
    }
}

/**
 * Debit as input for [ContractDetector]. Amounts are in minor units of the home currency;
 * debits are negative.
 */
data class ContractTransaction(
    val id: Long,
    val date: LocalDate,
    val amount: Long,
    val accountId: Long,
    val accountLabel: String? = null,
    val payeeId: Long? = null,
    val payeeName: String? = null,
    val comment: String? = null,
    val categoryPath: String? = null,
    val categoryIcon: String? = null,
    val templateId: Long? = null,
    /**
     * For a movement between own accounts: the account the money goes to. Set for transfers in the
     * app and for payments whose counterpart has the IBAN of an own account.
     */
    val targetAccountId: Long? = null,
)

/**
 * Whether money goes out of (contracts) or comes into (e.g. salary) the accounts
 */
enum class ContractDirection {
    EXPENSE, INCOME;

    companion object {
        fun of(amount: Long) = if (amount < 0) EXPENSE else INCOME
    }
}

/**
 * A detected recurring payment, a contract for debits or a regular income for credits.
 *
 * All amounts are positive and in minor units of the home currency.
 */
data class Contract(
    /**
     * Identifies the contract across detections, also when further debits are added or the price changes.
     * Built from the payee (or template) and the interval. Used to store decisions of the user.
     */
    val signature: String,
    /** Name derived from the debits */
    val name: String,
    /** Name given by the user, overrides [name] */
    val customName: String? = null,
    /** How the user wants the [area] to be determined */
    val areaChoice: AreaChoice = AreaChoice.Automatic,
    val interval: ContractInterval,
    /** Payments that make up this contract, oldest first */
    val transactions: List<ContractTransaction>,
    val nextExpectedDate: LocalDate,
    /** False, if the contract has not been debited for longer than its interval */
    val isActive: Boolean,
    val direction: ContractDirection = ContractDirection.EXPENSE,
    /** True, if a [ContractRule] of the user defines the contract, false for a suggestion of [ContractDetector] */
    val isConfirmed: Boolean = false,
    /** For a suggestion: detected with so much certainty that it is confirmed without asking the user */
    val isConfident: Boolean = false,
    /** The rule that defines a confirmed contract */
    val rule: ContractRule? = null,
    /** When the user marked the contract as cancelled, see [ContractRule.cancelledOn] */
    val cancelledOn: LocalDate? = null,
    /** Whether the user declared the contract as reserve, null for the suggestion, see [isReserve] */
    val reserveChoice: Boolean? = null,
) {
    val displayName: String get() = customName ?: name

    /** [displayName], or a placeholder if it is empty */
    val title: String get() = displayName.ifEmpty { NO_NAME }

    val isCancelled: Boolean get() = cancelledOn != null

    /** Puts money aside instead of spending it, see [Reserve] */
    val isReserve: Boolean get() = !isIncome && (reserveChoice ?: (isOwnTransfer || Reserve.suggests(this)))

    /** Money moved to an own account, e.g. a standing order to a savings account */
    val isOwnTransfer: Boolean get() = lastTransaction.targetAccountId != null

    /** False for a cancelled contract only known from its [ContractRule.snapshot] */
    val hasPayments: Boolean get() = transactions.any { it.id != SNAPSHOT_TRANSACTION_ID }

    /** Area suggested by the category of the last debit */
    val suggestedArea: ContractArea? get() = BuiltInArea.suggest(categoryPath)

    val area: ContractArea?
        get() = when (val choice = areaChoice) {
            AreaChoice.Automatic -> suggestedArea
            is AreaChoice.Fixed -> choice.area
        }

    val lastTransaction: ContractTransaction get() = transactions.last()
    val lastDate: LocalDate get() = lastTransaction.date
    val lastAmount: Long get() = lastTransaction.amount.absoluteValue

    /** Amount of the payment before the last one, null if there is none */
    val previousAmount: Long? get() = transactions.getOrNull(transactions.size - 2)?.amount?.absoluteValue

    val isIncome: Boolean get() = direction == ContractDirection.INCOME

    val categoryPath: String? get() = lastTransaction.categoryPath
    val categoryIcon: String? get() = lastTransaction.categoryIcon
    val accountLabel: String? get() = lastTransaction.accountLabel

    val yearlyAmount: Long get() = lastAmount * interval.perYear
    val monthlyAmount: Long get() = yearlyAmount / 12
}

/**
 * Builds a contract from its payments with a known interval
 *
 * @param transactions sorted by date
 */
internal fun contractOf(
    signature: String,
    transactions: List<ContractTransaction>,
    interval: ContractInterval,
    direction: ContractDirection,
    today: LocalDate,
    isConfirmed: Boolean = false,
): Contract {
    val last = transactions.last()
    val daysSinceLast = ChronoUnit.DAYS.between(last.date, today)
    val grace = interval.graceDays()
    return Contract(
        signature = signature,
        name = transactions.asReversed().firstNotNullOfOrNull { it.payeeName?.takeIf(String::isNotBlank) }
            ?: last.comment?.takeIf(String::isNotBlank)
            ?: last.categoryPath
            ?: "",
        interval = interval,
        transactions = transactions,
        nextExpectedDate = last.date.plus(interval.step),
        isActive = daysSinceLast <= interval.maxDays + grace,
        direction = direction,
        isConfirmed = isConfirmed
    )
}

/** Id of the payment that stands for the last one of a [ContractRule.snapshot] */
const val SNAPSHOT_TRANSACTION_ID = -1L

/**
 * Days beyond [ContractInterval.maxDays] without payment until a contract counts as ended
 */
internal fun ContractInterval.graceDays() = max(ContractDetector.MIN_GRACE_DAYS, maxDays / 10)
