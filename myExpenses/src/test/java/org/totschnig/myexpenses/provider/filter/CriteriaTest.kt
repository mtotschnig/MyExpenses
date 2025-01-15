package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.model.CrStatus
import java.time.LocalDate

class CriteriaTest {



    @Test
    fun testAccountCriterionTransformation() {

    }

    @Test
    fun testTransferCriterion() {
        val roundTrip = TransferCriterion.fromStringExtra(TransferCriterion("Bank", 1L).toString())
        assertThat(roundTrip!!.label).isEqualTo("Bank")
        assertThat(roundTrip.values).containsExactly( 1L)
        assertThat(roundTrip.getSelection(false)).isEqualTo("transfer_peer IS NOT NULL AND (transfer_account IN (?) OR account_id IN (?))")
        assertThat(roundTrip.selectionArgs).asList().containsExactly("1","1")
    }

    @Test
    fun testCrStatusCriterion() {
        val roundTrip = CrStatusCriterion.fromStringExtra(CrStatusCriterion(listOf(CrStatus.VOID)).toString())
        assertThat(roundTrip.values).containsExactly(CrStatus.VOID)
    }

    @Test
    fun parseToNullOnInvalidInput() {
        assertThat(IdCriterion.parseStringExtra("Bank;;")).isNull()
    }
}
