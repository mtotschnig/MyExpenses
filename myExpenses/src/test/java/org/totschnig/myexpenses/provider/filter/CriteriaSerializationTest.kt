package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.totschnig.myexpenses.model.CrStatus
import java.time.LocalDate

class CriteriaSerializationTest {
    @Test
    fun testRoundTrip() {
        val criterion: Criterion = AndCriterion(
            setOf(
                NotCriterion(
                    OrCriterion(
                        setOf(
                            AccountCriterion("Bank", 1L),
                            AmountCriterion(Operation.EQ, listOf(1), "EUR", true),
                            CategoryCriterion("Food", 1),
                            CommentCriterion("suche"),
                            CrStatusCriterion(listOf(CrStatus.VOID)),
                            DateCriterion(LocalDate.now(), LocalDate.now().plusDays(1)),
                            MethodCriterion("CHEQUE", 1),
                            PayeeCriterion("Joe", 1),
                            TagCriterion("Tag", 1),
                            TransferCriterion("Bank", 1L),
                        )
                    )
                )
            )
        )
        val encodeToString = Json.encodeToString(criterion)
        print(encodeToString)
        Truth.assertThat(
            Json.decodeFromString<Criterion>(encodeToString)
        ).isEqualTo(criterion)
    }
}