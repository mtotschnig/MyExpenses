package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.printToLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.compose.TEST_TAG_GROUP_SUMMARY
import org.totschnig.myexpenses.compose.TEST_TAG_GROUP_SUMS
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.setGrouping
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import java.time.LocalDateTime
import java.time.ZoneOffset

@TestShard3
class GroupedHeaderTest : BaseMyExpensesTest() {
    private lateinit var account: Account

    @Before
    fun fixture() {
        account = buildAccount("Test account 1", openingBalance = 1000)
        repository.setGrouping(account.id, Grouping.MONTH)
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        val date = LocalDateTime.of(2024, 1, 22, 12, 0)
        op0.date = date.toEpochSecond(ZoneOffset.UTC)
        op0.amount = Money(CurrencyUnit.DebugInstance, -100L)
        op0.save(contentResolver)
        op0.amount = Money(CurrencyUnit.DebugInstance, -200L)
        op0.date = date.minusMonths(1).toEpochSecond(ZoneOffset.UTC)
        op0.saveAsNew(contentResolver)
        launch(account.id)
    }

    @After
    fun cleanup() {
        repository.deleteAccount(account.id)
    }

    @Test
    fun testCalculatedHeaderAmounts() {
        testGroupHeader(Grouping.groupId(2023, 11), 1000, 0, -200, 0)
        testGroupHeader(Grouping.groupId(2024, 0), 800, 0, -100, 0)
    }

    private fun testGroupHeader(
        headerId: Int,
        start: Long,
        income: Long,
        expense: Long,
        transfer: Long
    ) {
        val parent = listNodeUnmerged.onChildren()
            .filter(hasHeaderId(headerId))
            .onFirst()
        parent.printToLog("DEBUGG")
        val header = parent.onChildren()
        val firstSummary = header
            .filter(hasTestTag(TEST_TAG_GROUP_SUMMARY)).onFirst().onChildren()
        val firstSums = header.filter(hasTestTag(TEST_TAG_GROUP_SUMS)).onFirst().onChildren()
        val delta = income + expense + transfer
        firstSummary[0].assert(hasAmount(start))
        firstSummary[1].assert(hasAmount(delta))
        firstSummary[2].assert(hasAmount(start+delta))
        firstSums[0].assert(hasAmount(income))
        firstSums[1].assert(hasAmount(expense))
        firstSums[2].assert(hasAmount(transfer))
    }
}