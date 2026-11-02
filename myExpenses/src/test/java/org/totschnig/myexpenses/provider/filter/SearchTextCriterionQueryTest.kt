package org.totschnig.myexpenses.provider.filter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model2.Account

/**
 * Runs the search against the database. Each transaction has an amount of its own bit, so the sum
 * tells which ones were found.
 */
@RunWith(AndroidJUnit4::class)
class SearchTextCriterionQueryTest : BaseTestWithRepository() {

    private lateinit var account: Account

    @Before
    fun insertData() {
        account = Account(
            label = "Account",
            currency = CurrencyUnit.DebugInstance.code,
            type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        ).createIn(repository)
        val payee = repository.createParty("Müller Bäckerei")!!.id
        val groceries = writeCategory("Lebensmittel")
        val bakery = writeCategory("Brot", groceries)
        repository.insertTransaction(account.id, -1, payeeId = payee)
        repository.insertTransaction(account.id, -2, categoryId = bakery)
        repository.insertTransaction(account.id, -4, comment = "Kino mit Anna")
        repository.insertTransaction(account.id, -8, referenceNumber = "INV-42")
        repository.insertTransaction(account.id, -1250)
    }

    private fun sum(criterion: Criterion) = repository.getTransactionSum(account, criterion)

    @Test
    fun findsByPayeeIgnoringCaseAndAccents() {
        assertThat(sum(SearchTextCriterion("muller"))).isEqualTo(-1)
        assertThat(sum(SearchTextCriterion("BÄCKEREI"))).isEqualTo(-1)
    }

    @Test
    fun findsByCategoryAndTheCategoryAbove() {
        assertThat(sum(SearchTextCriterion("Brot"))).isEqualTo(-2)
        assertThat(sum(SearchTextCriterion("Lebensmittel"))).isEqualTo(-2)
    }

    @Test
    fun findsByNoteAndReferenceNumber() {
        assertThat(sum(SearchTextCriterion("anna"))).isEqualTo(-4)
        assertThat(sum(SearchTextCriterion("inv-42"))).isEqualTo(-8)
    }

    @Test
    fun findsByAmount() {
        assertThat(sum(SearchTextCriterion("12,50", 1250))).isEqualTo(-1250)
    }

    @Test
    fun canBeExcluded() {
        assertThat(sum(NotCriterion(SearchTextCriterion("anna")))).isEqualTo(-1 - 2 - 8 - 1250)
    }
}
