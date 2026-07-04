package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.mockito.Mockito.mock
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.nest

class AccountNestingTest {

    @Test
    fun testNesting() {
        val viewModel = MyExpensesV2ViewModel(mock(android.app.Application::class.java), SavedStateHandle())
        
        val accounts = listOf(
            createAccount(1, "Portfolio 1", null, isPortfolio = true),
            createAccount(2, "Sub Account 1", 1),
            createAccount(3, "Sub Account 2", 1),
            createAccount(4, "Standalone Account", null)
        )

        val nested = with(viewModel) { accounts.nest() }

        assertThat(nested).hasSize(2)
        
        val p1 = nested.find { it.id == 1L }!!
        assertThat(p1.children).hasSize(2)
        assertThat(p1.children[0].id).isEqualTo(2L)
        assertThat(p1.children[1].id).isEqualTo(3L)

        val standalone = nested.find { it.id == 4L }!!
        assertThat(standalone.children).isEmpty()
    }

    private fun createAccount(id: Long, label: String, parentId: Long?, isPortfolio: Boolean = false) = FullAccount(
        id = id,
        label = label,
        currencyUnit = CurrencyUnit("EUR", "€", 2),
        type = AccountType.CASH,
        parentId = parentId,
        isPortfolio = isPortfolio
    )
}
