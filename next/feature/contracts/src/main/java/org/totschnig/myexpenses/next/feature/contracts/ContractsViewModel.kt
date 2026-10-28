package org.totschnig.myexpenses.next.feature.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource
import org.totschnig.myexpenses.next.core.common.stateWhileShown
import org.totschnig.myexpenses.next.data.contracts.AreaChoice
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractAnalysis
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.ContractRepository
import org.totschnig.myexpenses.next.data.contracts.ContractRule
import org.totschnig.myexpenses.next.data.contracts.ContractSettings
import org.totschnig.myexpenses.next.data.contracts.ContractStore
import org.totschnig.myexpenses.next.data.contracts.ContractsOverview
import org.totschnig.myexpenses.next.data.contracts.CustomArea
import org.totschnig.myexpenses.next.data.contracts.PaymentContractInfo
import org.totschnig.myexpenses.next.data.contracts.SalaryChoice
import org.totschnig.myexpenses.next.data.contracts.dismissing
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.next.data.contracts.infoFor
import org.totschnig.myexpenses.next.data.contracts.marking
import org.totschnig.myexpenses.next.data.contracts.merging
import org.totschnig.myexpenses.next.data.contracts.restoring
import org.totschnig.myexpenses.next.data.contracts.withRuleFor

/**
 * The contracts and incomes of [repository] for the screens, and what the user does with them.
 * Decisions of the user (dismissed contracts, custom names, categories) go into its store.
 */
class ContractsViewModel(
    private val repository: ContractRepository,
    /** The home currency, which the user can change */
    val homeCurrency: StateFlow<CurrencyInfo>,
) : ViewModel(), ContractActions {

    val ledgerSource: LedgerSource
        get() = repository.ledgerSource

    private val store: ContractStore
        get() = repository.store

    /** News about confirmed contracts, newest first, see [ContractNews] */
    val news: StateFlow<List<ContractNews>> = repository.news.stateWhileShown(viewModelScope, emptyList())

    override fun markNewsRead(ids: Set<String>) {
        edit { editor ->
            editor.news = editor.news.map { if (it.id in ids) it.copy(isRead = true) else it }
        }
    }

    /** Contracts, i.e. recurring debits */
    val state: StateFlow<AnalysisState<ContractsOverview>> =
        repository.expenses.stateWhileShown(viewModelScope, AnalysisState.Loading)

    /** Recurring credits, e.g. salary */
    val incomeState: StateFlow<AnalysisState<ContractsOverview>> =
        repository.incomes.stateWhileShown(viewModelScope, AnalysisState.Loading)

    /**
     * Incomes chosen as salary are confirmed, so that the choice refers to their rules
     */
    override fun setSalary(choice: SalaryChoice) {
        updateRules { rules, analysis, editor ->
            var updated = rules
            val salary = if (choice is SalaryChoice.Fixed) SalaryChoice.Fixed(
                choice.signatures.mapTo(HashSet()) { signature ->
                    val suggestion = analysis.suggestions.find { it.signature == signature }
                    if (suggestion == null) signature
                    else updated.withRuleFor(suggestion, analysis.transactions).let { (all, rule) ->
                        updated = all
                        rule.key
                    }
                }
            ) else choice
            editor.setSalary(salary)
            updated
        }
    }

    /** Confirms a suggestion */
    override fun confirm(contract: Contract) {
        changeRule(contract) { it }
    }

    /** Declares the payments of [contract] as no contract */
    override fun dismiss(contract: Contract) {
        updateRules { rules, analysis, _ -> rules.dismissing(contract, analysis.transactions) }
    }

    override fun restore(contract: Contract) {
        updateRules { rules, _, editor ->
            editor.restoreDismissedBySignature(contract.signature)
            rules.restoring(contract)
        }
    }

    /**
     * Confirms a suggestion
     *
     * @param name blank to go back to the detected name
     */
    override fun rename(contract: Contract, name: String) {
        val trimmed = name.trim()
        changeRule(contract) { it.copy(name = trimmed.takeIf { name -> name.isNotEmpty() && name != contract.name }) }
    }

    /** Confirms a suggestion */
    override fun setIcon(contract: Contract, icon: String?) {
        changeRule(contract) { it.copy(icon = icon) }
    }

    /** Confirms a suggestion */
    override fun setArea(contract: Contract, choice: AreaChoice) {
        changeRule(contract) {
            it.copy(
                areaKey = when (choice) {
                    AreaChoice.Automatic -> null
                    is AreaChoice.Fixed -> choice.area?.key ?: ContractSettings.AREA_NONE
                }
            )
        }
    }

    /**
     * The user cancelled the contract (or an income was discontinued): it is not expected anymore,
     * but stays visible
     */
    override fun cancel(contract: Contract) {
        changeRule(contract) {
            it.copy(
                cancelledOn = LocalDate.now(),
                snapshot = ContractRule.Snapshot(contract.name, contract.lastAmount, contract.lastDate)
            )
        }
    }

    override fun revokeCancellation(contract: Contract) {
        changeRule(contract) { it.copy(cancelledOn = null, snapshot = null) }
    }

    /** Declares whether the contract puts money aside, which confirms a suggestion */
    override fun setReserve(contract: Contract, isReserve: Boolean) {
        changeRule(contract) { it.copy(reserve = isReserve) }
    }

    override fun setInterval(contract: Contract, interval: ContractInterval) {
        changeRule(contract) { it.copy(interval = interval) }
    }

    /** Takes back that the payments of [payeeId] belong to [contract], e.g. after a wrong merge */
    override fun removePayee(contract: Contract, payeeId: Long) {
        changeRule(contract) { it.copy(payeeIds = it.payeeIds - payeeId) }
    }

    /** Takes back that the movements to [accountId] belong to [contract], e.g. after a wrong merge */
    override fun removeTargetAccount(contract: Contract, accountId: Long) {
        changeRule(contract) { it.copy(targetAccountIds = it.targetAccountIds - accountId) }
    }

    /**
     * @param range absolute amounts in minor units, null for all amounts
     */
    override fun setAmountRange(contract: Contract, range: LongRange?) {
        changeRule(contract) { it.copy(amountRange = range) }
    }

    /** Joins [other] into [contract], see [merging] */
    override fun merge(contract: Contract, other: Contract) {
        updateRules { rules, analysis, editor ->
            val (merged, rule) = rules.merging(contract, other, analysis.transactions)
            editor.replaceSalary(other.signature, rule.key)
            merged
        }
    }

    /** What the analysis knows about a payment, see [PaymentContractInfo] */
    fun paymentInfo(transactionId: Long): Flow<PaymentContractInfo> =
        combine(ledgerSource.consent, repository.analysis) { consent, analysis ->
            when {
                analysis != null -> analysis.infoFor(transactionId)
                consent == true -> PaymentContractInfo.Loading
                else -> PaymentContractInfo.Unavailable
            }
        }

    /** Declares a payment as part of a contract paid every [interval] */
    fun markAsContract(transactionId: Long, interval: ContractInterval) {
        updateRules { rules, analysis, _ -> rules.marking(transactionId, interval, analysis.transactions, analysis.today) }
    }

    /** Changes the rule of [contract], which confirms a suggestion */
    private fun changeRule(contract: Contract, change: (ContractRule) -> ContractRule) {
        updateRules { rules, analysis, _ -> rules.withRuleFor(contract, analysis.transactions, change).first }
    }

    /**
     * Changes the rules based on the latest analysis. Does nothing before there is one,
     * since actions can only be triggered on contracts shown from it.
     */
    private fun updateRules(
        change: (List<ContractRule>, ContractAnalysis, ContractStore.Editor) -> List<ContractRule>,
    ) {
        val analysis = repository.latestAnalysis.value ?: return
        edit { editor -> editor.rules = change(editor.rules, analysis, editor) }
    }

    /**
     * @return the new category, so that a contract can be put into it right away
     */
    override fun createArea(name: String): CustomArea {
        val area = CustomArea(UUID.randomUUID().toString(), name.trim())
        edit { editor -> editor.customAreas += area }
        return area
    }

    override fun renameArea(area: CustomArea, name: String) {
        edit { editor ->
            editor.customAreas = editor.customAreas.map { if (it.id == area.id) it.copy(name = name.trim()) else it }
        }
    }

    /**
     * Contracts in the deleted category go back to their suggested category
     */
    override fun deleteArea(area: CustomArea) {
        edit { editor ->
            editor.customAreas = editor.customAreas.filter { it.id != area.id }
            editor.removeAreaBySignature(area.key)
            editor.rules = editor.rules.map { if (it.areaKey == area.key) it.copy(areaKey = null) else it }
        }
    }

    private fun edit(block: (ContractStore.Editor) -> Unit) {
        viewModelScope.launch { store.edit(block) }
    }
}
