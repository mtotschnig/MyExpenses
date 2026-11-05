package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS_FTS

class FtsCriterionTest {

    @Test
    fun testSelectionQueryIncludeAccountLabel() {
        val criterion = FtsCriterion("coffee", includeAccountLabel = true)
        val selection = criterion.getSelection(false)
        assertThat(selection).isEqualTo(
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        )
        assertThat(criterion.values).containsExactly("coffee")
    }

    @Test
    fun testSelectionQueryExcludeAccountLabel() {
        val criterion = FtsCriterion("coffee", includeAccountLabel = false)
        val selection = criterion.getSelection(false)
        assertThat(selection).isEqualTo(
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE " +
                "comment MATCH ? OR payee_name MATCH ? OR category_name MATCH ? OR " +
                "tag_names MATCH ? OR reference_number MATCH ?)"
        )
        assertThat(criterion.values).containsExactly(
            "coffee",
            "coffee",
            "coffee",
            "coffee",
            "coffee"
        )
    }

    @Test
    fun testNullOrBlankSelectionQuery() {
        val criterion = FtsCriterion("")
        assertThat(criterion.getSelection(false)).isEqualTo("1 = 1")
    }

    @Test
    fun testSanitizeSingleWordQuery() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("coffee")
        assertThat(sanitized).isEqualTo("coffee")
    }

    @Test
    fun testSanitizeNonAsciiUmlautWord() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("Österreich")
        assertThat(sanitized).isEqualTo("Österreich")
    }

    @Test
    fun testSanitizeExplicitWildcardPreservesWildcard() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("Öster*")
        assertThat(sanitized).isEqualTo("Öster*")
    }

    @Test
    fun testSanitizeQuotedPhrasePreservesQuotes() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("\"office supplies\"")
        assertThat(sanitized).isEqualTo("\"office supplies\"")
    }

    @Test
    fun testSanitizeTokenWithColonEnclosesInQuotes() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("payee_name:xxx")
        assertThat(sanitized).isEqualTo("\"payee_name:xxx\"")
    }

    @Test
    fun testSanitizeMultipleWords() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("coffee starbucks")
        assertThat(sanitized).isEqualTo("coffee starbucks")
    }

    @Test
    fun testSanitizeMultipleWordsWithUmlauts() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("Österreich Wien")
        assertThat(sanitized).isEqualTo("Österreich Wien")
    }
}
