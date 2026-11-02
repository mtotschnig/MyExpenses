package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class SearchTextCriterionTest {

    private fun placeholders(selection: String) = selection.count { it == '?' }

    @Test
    fun hasAnArgumentForEachPlaceholder() {
        listOf(SearchTextCriterion("Rewe"), SearchTextCriterion("12,50", 1250)).forEach { criterion ->
            listOf(false, true).forEach { queryParts ->
                val selection = if (queryParts) criterion.getSelectionForParts("t") else criterion.getSelectionForParents("t")
                assertThat(criterion.getSelectionArgs(queryParts)).hasLength(placeholders(selection))
            }
        }
    }

    @Test
    fun searchesPayeesByTheirNormalizedName() {
        val args = SearchTextCriterion(" Müller ").selectionArgs
        assertThat(args[0]).isEqualTo("%Müller%")
        assertThat(args[2]).isEqualTo("%muller%")
    }

    @Test
    fun escapesWildcards() {
        assertThat(SearchTextCriterion("50%").selectionArgs[0]).isEqualTo("%50\\%%")
    }

    @Test
    fun findsTheAmountAsExpenseAndIncome() {
        assertThat(SearchTextCriterion("12,50", 1250).selectionArgs.takeLast(2)).containsExactly("1250", "-1250")
        assertThat(SearchTextCriterion("Rewe").getSelection(false)).doesNotContain("amount")
    }

    @Test
    fun roundTrip() {
        val criterion: Criterion = AndCriterion(setOf(SearchTextCriterion("12,50", 1250), CommentCriterion("x")))
        assertThat(Json.decodeFromString<Criterion>(Json.encodeToString(criterion))).isEqualTo(criterion)
    }
}
