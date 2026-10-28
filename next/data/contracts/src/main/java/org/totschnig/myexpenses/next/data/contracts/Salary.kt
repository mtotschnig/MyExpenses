package org.totschnig.myexpenses.next.data.contracts


/**
 * Which regular incomes are salaries. The highest one determines the period of the monthly balance,
 * the others are expected as income within it.
 */
sealed interface SalaryChoice {
    /** The highest active monthly income */
    data object Automatic : SalaryChoice

    /** No salary, the balance uses the calendar month */
    data object None : SalaryChoice

    /** Chosen by the user, e.g. for people with more than one job */
    data class Fixed(val signatures: Set<String>) : SalaryChoice
}

/**
 * The salary suggested among these contracts: the highest active monthly income
 */
fun List<Contract>.automaticSalary(): Contract? =
    filter { it.isIncome && it.isActive && it.interval == ContractInterval.MONTHLY }
        .maxByOrNull { it.lastAmount }

/**
 * The salaries according to [choice], highest first. Chosen incomes that are no longer received
 * are left out; if none is left, the automatic choice applies.
 */
fun List<Contract>.salaries(choice: SalaryChoice): List<Contract> = when (choice) {
    SalaryChoice.Automatic -> listOfNotNull(automaticSalary())
    SalaryChoice.None -> emptyList()
    is SalaryChoice.Fixed -> filter { it.signature in choice.signatures && it.isIncome && it.isActive }
        .sortedByDescending { it.lastAmount }
        .ifEmpty { listOfNotNull(automaticSalary()) }
}
