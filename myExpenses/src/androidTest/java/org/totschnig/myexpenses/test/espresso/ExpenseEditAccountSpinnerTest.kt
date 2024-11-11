package org.totschnig.myexpenses.test.espresso

import android.widget.Spinner
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency
import kotlin.test.Test

class ExpenseEditAccountSpinnerTest: BaseExpenseEditTest() {
    private lateinit var account2: Account
    private lateinit var hiddenAccount: Account
    private lateinit var currency1: CurrencyUnit
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account1 = Account(label = "Test label 1", currency = currency1.code).createIn(repository)
        account2 =
            Account(label = "Test label 2", currency = currency2.code, type = AccountType.BANK)
                .createIn(repository)
        hiddenAccount =
            Account(label = "AAA", currency = "JPY").createIn(repository).also {
                repository.updateAccount(it.id) {
                    put(KEY_HIDDEN, true)
                }
            }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteAccount(hiddenAccount.id)
        }
    }

    @Test
    fun hiddenAccountIsSortedLast() {
        launch(intentForNewTransaction)
        testScenario.onActivity {
            with(it.findViewById<Spinner>(R.id.Account).adapter) {
                listOf(account1, account2, hiddenAccount).forEachIndexed { index, account ->
                    Truth.assertThat((getItem(index) as org.totschnig.myexpenses.viewmodel.data.Account).label).isEqualTo(account.label)
                }
            }
        }
    }
}