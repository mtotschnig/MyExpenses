package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.getParty
import org.totschnig.myexpenses.db2.getPayeeForTemplate
import org.totschnig.myexpenses.db2.getPayeeForTransaction
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model2.Party

@RunWith(RobolectricTestRunner::class)
class PartyListViewModelTest: BaseTestWithRepository() {

    private fun buildViewModel() = PartyListViewModel(
        application,
        SavedStateHandle()
    ).also {
        application.injector.inject(it)
    }

    @Test
    fun testMergeStrategyGroup() {
        val viewModel = buildViewModel()
        val partyA = repository.requireParty("A")!!
        val partyB = repository.requireParty("B")!!
        val partyC = repository.requireParty("C")!!
        viewModel.mergeParties(setOf(partyB, partyC), partyA, MergeStrategy.GROUP)
        assertThat(repository.getParty(partyA)!!.parentId).isNull()
        assertThat(repository.getParty(partyB)!!.parentId).isEqualTo(partyA)
        assertThat(repository.getParty(partyC)!!.parentId).isEqualTo(partyA)
    }

    @Test
    fun testMergeStrategyDelete() {
        val viewModel = buildViewModel()
        val partyA = repository.requireParty("A")!!
        val partyB = repository.requireParty("B")!!
        val partyC = repository.requireParty("C")!!
        val accountId = insertAccount("Test account", currency = "EUR")
        val (transactionId, _) = insertTransaction(accountId, 123, payeeId = partyB)
        val templateId = insertTemplate(accountId, "Test template", 123, payeeId = partyC)
        viewModel.mergeParties(setOf(partyB, partyC), partyA, MergeStrategy.DELETE)
        assertThat(repository.getParty(partyA)!!.parentId).isNull()
        assertThat(repository.getParty(partyB)).isNull()
        assertThat(repository.getParty(partyC)).isNull()
        assertThat(repository.getPayeeForTransaction(transactionId)).isEqualTo(partyA)
        assertThat(repository.getPayeeForTemplate(templateId)).isEqualTo(partyA)
    }

    @Test
    fun testMergeDeleteGroupedParties() {
        val viewModel = buildViewModel()
        val partyA = repository.createParty("A")!!.id
        val partyB = repository.createParty(Party(name = "B"))!!.id
        val partyC = repository.createParty(Party(name = "C", parentId = partyB))!!.id
        val accountId = insertAccount("Test account", currency = "EUR")
        val (transactionId, _) = insertTransaction(accountId, 123, payeeId = partyC)
        val templateId = insertTemplate(accountId, "Test template", 123, payeeId = partyC)
        viewModel.mergeParties(setOf(partyB), partyA, MergeStrategy.DELETE)
        assertThat(repository.getParty(partyA)!!.parentId).isNull()
        assertThat(repository.getParty(partyB)).isNull()
        assertThat(repository.getParty(partyC)).isNull()
        assertThat(repository.getPayeeForTransaction(transactionId)).isEqualTo(partyA)
        assertThat(repository.getPayeeForTemplate(templateId)).isEqualTo(partyA)
    }
}