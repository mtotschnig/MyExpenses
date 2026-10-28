package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class TransactionIdCriterionTest {

    private val criterion = TransactionIdCriterion("Netflix", listOf(3L, 5L, 8L))

    @Test
    fun selectsGivenTransactionsOnly() {
        // Neither split parts nor archives are searched, so the arguments are not repeated
        assertThat(criterion.getSelectionForParents("t")).isEqualTo("(_id IN (?,?,?))")
        assertThat(criterion.getSelectionArgs(false)).asList().containsExactly("3", "5", "8").inOrder()
    }

    @Test
    fun roundTrip() {
        val encoded = Json.encodeToString<Criterion>(criterion)
        assertThat(Json.decodeFromString<Criterion>(encoded)).isEqualTo(criterion)
    }
}
