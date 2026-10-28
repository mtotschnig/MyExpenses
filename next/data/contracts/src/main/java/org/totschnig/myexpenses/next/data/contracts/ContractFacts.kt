package org.totschnig.myexpenses.next.data.contracts

/**
 * What other features know about contracts, e.g. the monthly balance
 *
 * @param active confirmed contracts, i.e. recurring debits, that are paid now
 * @param confirmed confirmed contracts whose payments count, also ended and cancelled ones
 * @param salaries regular incomes that are salaries, highest first
 * @param paymentIds ids of the payments of all contracts and incomes, suggestions included
 * @param transactions those contracts were detected from, see [contractTransactions]
 */
data class ContractFacts(
    val active: List<Contract>,
    val confirmed: List<Contract>,
    val salaries: List<Contract>,
    val paymentIds: Set<Long>,
    val transactions: List<ContractTransaction>,
)
