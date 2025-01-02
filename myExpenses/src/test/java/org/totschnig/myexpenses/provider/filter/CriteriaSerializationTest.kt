package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class CriteriaSerializationTest {
    @Test
    fun testRoundTrip() {
        val criterion: BaseCriterion = AndCriterion(
            listOf(
                NotCriterion(
                    OrCriterion(
                        listOf(
                            CommentCriterion("suche")
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