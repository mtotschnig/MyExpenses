package org.totschnig.myexpenses.test.espresso

import android.widget.Spinner
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.SpinnerItem
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountFlag
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.setAccountProperty
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_FAVORITE
import org.totschnig.myexpenses.model.PREDEFINED_NAME_INACTIVE
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency
import kotlin.test.Test

@TestShard2
class ExpenseEditAccountSpinnerTest: BaseExpenseEditTest() {
    private lateinit var favoriteAccount: Account
    private lateinit var hiddenAccount: Account
    private lateinit var currency1: CurrencyUnit
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        val inactive = repository.findAccountFlag(PREDEFINED_NAME_INACTIVE)!!.id
        val favorite = repository.findAccountFlag(PREDEFINED_NAME_FAVORITE)!!.id
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        val type = repository.findAccountType(PREDEFINED_NAME_BANK)!!
        account1 = Account(label = "Test label 1", currency = currency1.code, type = type).createIn(repository)
        favoriteAccount =
            Account(label = "ZZZ", currency = currency2.code, type = type)
                .createIn(repository)
        repository.setAccountProperty(favoriteAccount.id, KEY_FLAG, favorite)
        hiddenAccount =
            Account(label = "AAA", currency = "JPY", type = type).createIn(repository)
        repository.setAccountProperty(hiddenAccount.id, KEY_FLAG, inactive)

    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(favoriteAccount.id)
            repository.deleteAccount(hiddenAccount.id)
        }
    }

    @Test
    fun hiddenAccountIsSortedLast() {
        launch()
        testScenario.onActivity {
            val expectedOrder = listOf(favoriteAccount.id, account1.id, hiddenAccount.id)
            val actualOrder = buildList {
                with(it.findViewById<Spinner>(R.id.Account).adapter) {
                    for (index in 0 until count) {
                        val item = getItem(index)
                        if (item is SpinnerItem.Item<*>) {
                            add(item.data.id)
                        }
                    }
                }
            }
            assertThat(actualOrder).containsExactlyElementsIn(expectedOrder).inOrder()
        }
    }
}