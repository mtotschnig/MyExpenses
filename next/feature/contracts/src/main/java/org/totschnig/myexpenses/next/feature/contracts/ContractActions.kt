package org.totschnig.myexpenses.next.feature.contracts

import org.totschnig.myexpenses.next.data.contracts.AreaChoice
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.CustomArea
import org.totschnig.myexpenses.next.data.contracts.SalaryChoice
import org.totschnig.myexpenses.next.data.contracts.icon


/**
 * What the user can do with contracts and incomes on their screens, see [ContractsViewModel].
 * Changing a suggestion confirms it. Does nothing by default, e.g. in previews.
 */
interface ContractActions {
    /** Declares the payments of [contract] as no contract */
    fun dismiss(contract: Contract) {}

    fun restore(contract: Contract) {}

    /** @param name blank to go back to the detected name */
    fun rename(contract: Contract, name: String) {}

    fun setArea(contract: Contract, choice: AreaChoice) {}

    /** @param icon null to go back to the one shown automatically */
    fun setIcon(contract: Contract, icon: String?) {}

    /** @return the new category, so that a contract can be put into it right away */
    fun createArea(name: String): CustomArea = CustomArea(name, name)

    fun renameArea(area: CustomArea, name: String) {}

    /** Contracts in the deleted category go back to their suggested category */
    fun deleteArea(area: CustomArea) {}

    fun setSalary(choice: SalaryChoice) {}

    fun confirm(contract: Contract) {}

    fun setInterval(contract: Contract, interval: ContractInterval) {}

    /** Takes back that the payments of [payeeId] belong to [contract], e.g. after a wrong merge */
    fun removePayee(contract: Contract, payeeId: Long) {}

    /** Takes back that the movements to [accountId] belong to [contract], e.g. after a wrong merge */
    fun removeTargetAccount(contract: Contract, accountId: Long) {}

    /** @param range absolute amounts in minor units, null for all amounts */
    fun setAmountRange(contract: Contract, range: LongRange?) {}

    /** Joins [other] into [contract] */
    fun merge(contract: Contract, other: Contract) {}

    /** The user cancelled the contract (or an income was discontinued): it is not expected anymore */
    fun cancel(contract: Contract) {}

    fun revokeCancellation(contract: Contract) {}

    /** Declares whether the contract puts money aside */
    fun setReserve(contract: Contract, isReserve: Boolean) {}

    fun markNewsRead(ids: Set<String>) {}
}
