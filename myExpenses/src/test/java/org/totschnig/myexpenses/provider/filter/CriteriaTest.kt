package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.model.CrStatus
import java.time.LocalDate

class CriteriaTest {

    @Test
    fun testDateCriterion() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val roundTrip = DateCriterion.fromStringExtra(DateCriterion(today, tomorrow).toString())
        Truth.assertThat(roundTrip.values).asList().containsExactly(today, tomorrow)
    }

    @Test
    fun testCategoryCriterion() {
        val roundTrip = CategoryCriterion.fromStringExtra(CategoryCriterion("Housing", 1L).toString())
        Truth.assertThat(roundTrip!!.label).isEqualTo("Housing")
        Truth.assertThat(roundTrip.values).asList().containsExactly( 1L)
    }

    @Test
    fun testAccountCriterion() {
        val roundTrip = AccountCriterion.fromStringExtra(AccountCriterion("Bank", 1L).toString())
        Truth.assertThat(roundTrip!!.label).isEqualTo("Bank")
        Truth.assertThat(roundTrip.values).asList().containsExactly( 1L)
        Truth.assertThat(roundTrip.selection).isEqualTo("account_id IN (?) OR transfer_account IN (?)")
        Truth.assertThat(roundTrip.selectionArgs).asList().containsExactly("1","1")
    }

    @Test
    fun testTransferCriterion() {
        val roundTrip = TransferCriterion.fromStringExtra(TransferCriterion("Bank", 1L).toString())
        Truth.assertThat(roundTrip!!.label).isEqualTo("Bank")
        Truth.assertThat(roundTrip.values).asList().containsExactly( 1L)
        Truth.assertThat(roundTrip.selection).isEqualTo("transfer_peer IS NOT NULL AND (transfer_account IN (?) OR account_id IN (?))")
        Truth.assertThat(roundTrip.selectionArgs).asList().containsExactly("1","1")
    }

    @Test
    fun testCrStatusCriterion() {
        val roundTrip = CrStatusCriterion.fromStringExtra(CrStatusCriterion(arrayOf(CrStatus.VOID)).toString())
        Truth.assertThat(roundTrip.values).asList().containsExactly(CrStatus.VOID)
    }

    @Test
    fun parseToNullOnInvalidInput() {
        Truth.assertThat(IdCriterion.parseStringExtra("Bank;;")).isNull()
    }
}
