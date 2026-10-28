package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class ContractAnalysisTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private var nextId = 1L

    private fun monthly(count: Int, amount: Long, payeeId: Long, templateId: Long? = null, last: LocalDate = today.minusDays(3)) =
        (0 until count).map {
            ContractTransaction(
                id = nextId++, date = last.minus(Period.ofMonths(count - 1 - it)), amount = amount, accountId = 1,
                payeeId = payeeId, payeeName = "Payee $payeeId", templateId = templateId
            )
        }

    private val rent = monthly(12, -80000, payeeId = 1)
    private val gym = monthly(3, -3000, payeeId = 2)
    private val salary = monthly(12, 300000, payeeId = 3)
    private val transactions = rent + gym + salary

    private fun analysis(rules: List<ContractRule> = emptyList(), transactions: List<ContractTransaction> = this.transactions) =
        ContractAnalysis.of(transactions, rules, today)

    private fun ContractAnalysis.contract(payeeId: Long) =
        contracts.single { it.transactions.first().payeeId == payeeId }

    @Test
    fun confidentWithManyRegularPaymentsOrTemplate() {
        val analysis = analysis(transactions = transactions + monthly(3, -1000, payeeId = 4, templateId = 7))
        assertTrue(analysis.contract(1).isConfident)
        assertFalse(analysis.contract(2).isConfident)
        assertTrue(analysis.contract(3).isConfident)
        assertTrue(analysis.contract(4).isConfident)
    }

    @Test
    fun confirmsConfidentActiveSuggestions() {
        val ended = monthly(8, -1500, payeeId = 5, last = today.minusMonths(6))
        val analysis = analysis(transactions = transactions + ended)
        val rules = analysis.automaticRules(emptyList())
        assertEquals(setOf(1L, 3L), rules.flatMap { it.payeeIds }.toSet())
        assertTrue(rules.all { it.kind == ContractRule.Kind.CONTRACT })
    }

    @Test
    fun doesNotConfirmTwice() {
        val first = analysis().automaticRules(emptyList())
        assertTrue(analysis(first).automaticRules(first).isEmpty())
        // Also when the analysis did not know the stored rules yet
        assertTrue(analysis().automaticRules(first).isEmpty())
    }

    @Test
    fun rulesTurnSuggestionsIntoConfirmedContracts() {
        val rules = analysis().automaticRules(emptyList())
        val analysis = analysis(rules)
        assertTrue(analysis.contract(1).isConfirmed)
        assertFalse(analysis.contract(2).isConfirmed)
        assertEquals(listOf(analysis.contract(2)), analysis.suggestions)
    }

    @Test
    fun dismissingSuggestionIgnoresItsPayments() {
        val before = analysis()
        val rules = emptyList<ContractRule>().dismissing(before.contract(2), transactions)
        val after = analysis(rules)
        assertTrue(after.contracts.none { it.transactions.first().payeeId == 2L })
        assertEquals(gym, after.dismissed.single().transactions)
    }

    @Test
    fun dismissingConfirmedContractKeepsItsDecisions() {
        val (rules, rule) = emptyList<ContractRule>().withRuleFor(analysis().contract(1), transactions) {
            it.copy(name = "Rent")
        }
        val confirmed = analysis(rules).contract(1)
        val dismissed = rules.dismissing(confirmed, transactions)
        assertEquals(ContractRule.Kind.IGNORE, dismissed.single().kind)
        assertEquals("Rent", dismissed.single().name)
        assertEquals(rule.id, dismissed.single().id)
    }

    @Test
    fun restoringConfirms() {
        val rules = emptyList<ContractRule>().dismissing(analysis().contract(2), transactions)
        val restored = rules.restoring(analysis(rules).dismissed.single())
        assertEquals(ContractRule.Kind.CONTRACT, restored.single().kind)
        assertTrue(analysis(restored).contract(2).isConfirmed)
    }

    @Test
    fun withRuleForChangesExistingRule() {
        val (rules, _) = emptyList<ContractRule>().withRuleFor(analysis().contract(2), transactions)
        val confirmed = analysis(rules).contract(2)
        val (changed, rule) = rules.withRuleFor(confirmed, transactions) { it.copy(name = "Gym") }
        assertEquals(1, changed.size)
        assertEquals("Gym", rule.name)
    }

    @Test
    fun migratesDecisionsStoredBySignature() {
        val before = analysis()
        val rent = before.contract(1)
        val gym = before.contract(2)
        val salary = before.contract(3)
        val settings = ContractSettings(
            dismissed = setOf(gym.signature),
            names = mapOf(rent.signature to "Rent"),
            areas = mapOf(rent.signature to BuiltInArea.HOUSING.key),
            salary = SalaryChoice.Fixed(setOf(salary.signature, "gone"))
        )
        val (rules, salaryChoice) = migrateToRules(settings, before)
        assertEquals(3, rules.size)
        val rentRule = rules.single { 1L in it.payeeIds }
        assertEquals(ContractRule.Kind.CONTRACT, rentRule.kind)
        assertEquals("Rent", rentRule.name)
        assertEquals(BuiltInArea.HOUSING.key, rentRule.areaKey)
        assertEquals(ContractRule.Kind.IGNORE, rules.single { 2L in it.payeeIds }.kind)
        val salaryRule = rules.single { 3L in it.payeeIds }
        assertEquals(ContractRule.Kind.CONTRACT, salaryRule.kind)
        assertNull(salaryRule.name)
        assertEquals(SalaryChoice.Fixed(setOf(salaryRule.key)), salaryChoice)
    }

    @Test
    fun migrationWithoutDecisionsCreatesNoRules() {
        val (rules, salary) = migrateToRules(ContractSettings(), analysis())
        assertTrue(rules.isEmpty())
        assertEquals(SalaryChoice.Automatic, salary)
    }

    @Test
    fun stateTakesDecisionsFromRules() {
        val (rules, _) = emptyList<ContractRule>().withRuleFor(analysis().contract(1), transactions) {
            it.copy(name = "Rent", areaKey = BuiltInArea.HOUSING.key)
        }
        val ignored = rules.dismissing(analysis(rules).contract(2), transactions)
        val analysis = analysis(ignored)
        val state = buildContractsState(
            analysis.contracts.filter { !it.isIncome },
            ContractSettings(rules = ignored),
            analysis.dismissed
        )
        val rent = state.active.single { it.isConfirmed }
        assertEquals("Rent", rent.displayName)
        assertEquals(BuiltInArea.HOUSING, rent.area)
        assertEquals(listOf(gym), state.dismissed.map { it.transactions })
    }

    @Test
    fun mergingJoinsPayeesAndRemovesOtherRule() {
        // Provider renamed: the old contract ended, the new one runs since
        val old = monthly(8, -4000, payeeId = 6, last = today.minusMonths(6))
        val new = monthly(5, -4200, payeeId = 7)
        val transactions = old + new
        val (rules, _) = emptyList<ContractRule>().withRuleFor(analysis(transactions = transactions).contract(7), transactions) {
            it.copy(name = "Internet")
        }
        val before = analysis(rules, transactions)
        val (merged, rule) = rules.merging(before.contract(7), before.contract(6), transactions)
        assertEquals(1, merged.size)
        assertEquals(setOf(6L, 7L), rule.payeeIds)
        assertEquals("Internet", rule.name)
        val after = analysis(merged, transactions)
        val contract = after.contracts.single()
        assertEquals(13, contract.transactions.size)
        assertTrue(contract.isActive)
        assertEquals("Internet", contract.displayName)
    }

    @Test
    fun mergingDropsAmountRangeUnlessBothHaveOne() {
        val a = ContractRule("a", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, setOf(1), amountRange = 700L..1100L, interval = ContractInterval.MONTHLY)
        val b = ContractRule("b", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, setOf(2), amountRange = 1000L..1500L, interval = ContractInterval.MONTHLY)
        val c = ContractRule("c", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, setOf(3), interval = ContractInterval.MONTHLY)
        val payments = monthly(3, -900, payeeId = 1) + monthly(3, -1200, payeeId = 2) + monthly(3, -5000, payeeId = 3)
        val analysis = analysis(listOf(a, b, c), payments)
        val (_, ab) = listOf(a, b, c).merging(analysis.contract(1), analysis.contract(2), payments)
        assertEquals(700L..1500L, ab.amountRange)
        val (_, ac) = listOf(a, b, c).merging(analysis.contract(1), analysis.contract(3), payments)
        assertNull(ac.amountRange)
    }

    @Test
    fun infoForPayments() {
        val insurance = listOf(
            ContractTransaction(id = 900, date = today.minusDays(40), amount = -23000, accountId = 1, payeeId = 8, payeeName = "HUK")
        )
        val withoutPayee = ContractTransaction(id = 901, date = today.minusDays(5), amount = -500, accountId = 1)
        val payments = transactions + insurance + withoutPayee
        val confirmedRules = analysis(transactions = payments).automaticRules(emptyList())
        val rules = confirmedRules.dismissing(analysis(confirmedRules, payments).contract(2), payments)
        val analysis = analysis(rules, payments)
        assertTrue(analysis.infoFor(rent.first().id) is PaymentContractInfo.Confirmed)
        assertTrue(analysis.infoFor(gym.first().id) is PaymentContractInfo.Dismissed)
        assertEquals(PaymentContractInfo.Markable(isIncome = false), analysis.infoFor(900))
        assertEquals(PaymentContractInfo.NoPayee, analysis.infoFor(901))
        assertEquals(PaymentContractInfo.Unavailable, analysis.infoFor(12345))
    }

    @Test
    fun infoForSuggestedPayment() {
        // Too few payments to be confirmed automatically
        val analysis = analysis(transactions = gym)
        assertTrue(analysis.infoFor(gym.first().id) is PaymentContractInfo.Suggested)
    }

    @Test
    fun markingSinglePaymentCreatesContract() {
        // Yearly insurance, paid only once so far: never suggested
        val insurance = ContractTransaction(id = 900, date = today.minusDays(40), amount = -23000, accountId = 1, payeeId = 8, payeeName = "HUK")
        val payments = transactions + insurance
        val rules = emptyList<ContractRule>().marking(900, ContractInterval.YEARLY, payments, today)
        val contract = analysis(rules, payments).contracts.single { it.isConfirmed }
        assertEquals(ContractInterval.YEARLY, contract.interval)
        assertEquals(listOf(insurance), contract.transactions)
        assertTrue(contract.isActive)
        assertEquals(today.minusDays(40).plusYears(1), contract.nextExpectedDate)
    }
}
