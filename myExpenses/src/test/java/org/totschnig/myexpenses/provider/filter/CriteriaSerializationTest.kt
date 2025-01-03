package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.LocalDate

class CriteriaSerializationTest {
    @Test
    fun testRoundTrip() {
        val criterion: BaseCriterion = AndCriterion(
            listOf(
                NotCriterion(
                    OrCriterion(
                        listOf(
                            CommentCriterion("suche"),
                            AmountCriterion(WhereFilter.Operation.EQ, listOf(1), "EUR", true),
                            CategoryCriterion("Food", 1),
                            DateCriterion(LocalDate.now(), LocalDate.now().plusDays(1))
                        )
                    )
                )
            )
        )
        val encodeToString = Json.encodeToString(criterion)
        print(encodeToString)
        Truth.assertThat(
            Json.decodeFromString<BaseCriterion>(encodeToString)
        ).isEqualTo(criterion)
    }
}