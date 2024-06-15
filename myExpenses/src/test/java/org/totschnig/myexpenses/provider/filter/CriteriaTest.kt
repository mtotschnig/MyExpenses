package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.model.CrStatus
import java.time.LocalDate

class CriteriaTest {

    @Test
    fun testDateCriterion() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val roundTrip = DateCriterion.fromStringExtra(DateCriterion(today, tomorrow).toString())
        assertThat(roundTrip.values).asList().containsExactly(today, tomorrow)
    }

    @Test
    fun testCategoryCriterion() {
        val roundTrip = CategoryCriterion.fromStringExtra(CategoryCriterion("Housing", 1L).toString())
        assertThat(roundTrip!!.label).isEqualTo("Housing")
        assertThat(roundTrip.values).asList().containsExactly( 1L)
    }

    @Test
    fun testAccountCriterion() {
        val roundTrip = AccountCriterion.fromStringExtra(AccountCriterion("Bank", 1L).toString())
        assertThat(roundTrip!!.label).isEqualTo("Bank")
        assertThat(roundTrip.values).asList().containsExactly( 1L)
        assertThat(roundTrip.getSelection(false)).isEqualTo("account_id IN (?)")
        assertThat(roundTrip.selectionArgs).asList().containsExactly("1")
    }

    @Test
    fun testAccountCriterionTransformation() {

    }

    @Test
    fun testTransferCriterion() {
        val roundTrip = TransferCriterion.fromStringExtra(TransferCriterion("Bank", 1L).toString())
        assertThat(roundTrip!!.label).isEqualTo("Bank")
        assertThat(roundTrip.values).asList().containsExactly( 1L)
        assertThat(roundTrip.getSelection(false)).isEqualTo("transfer_peer IS NOT NULL AND (transfer_account IN (?) OR account_id IN (?))")
        assertThat(roundTrip.selectionArgs).asList().containsExactly("1","1")
    }

    @Test
    fun testCrStatusCriterion() {
        val roundTrip = CrStatusCriterion.fromStringExtra(CrStatusCriterion(arrayOf(CrStatus.VOID)).toString())
        assertThat(roundTrip.values).asList().containsExactly(CrStatus.VOID)
    }

    @Test
    fun parseToNullOnInvalidInput() {
        assertThat(IdCriterion.parseStringExtra("Bank;;")).isNull()
    }
}
