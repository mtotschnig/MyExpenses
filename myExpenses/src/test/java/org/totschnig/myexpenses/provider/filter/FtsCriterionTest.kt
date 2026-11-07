package org.totschnig.myexpenses.provider.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS_FTS

class FtsCriterionTest {

    @Test
    fun testSelectionQuery() {
        val criterion = FtsCriterion("coffee")
        val selection = criterion.getSelection(false)
        assertThat(selection).isEqualTo(
            "$KEY_ROWID IN (SELECT rowid FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        )
        assertThat(criterion.values).containsExactly("coffee")
    }

    @Test
    fun testStandaloneNegationSelectionQuery() {
        val criterion = FtsCriterion("-coffee")
        val selection = criterion.getSelection(false)
        assertThat(selection).isEqualTo(
            "$KEY_ROWID NOT IN (SELECT rowid FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        )
        assertThat(criterion.values).containsExactly("coffee")
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
    fun testSanitizeUnmatchedQuotesAutoBalances() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("\"office supplies")
        assertThat(sanitized).isEqualTo("\"office supplies\"")
    }

    @Test
    fun testSanitizePreservesColumnDirectives() {
        val sanitized =
            FtsCriterion.sanitizeFtsQuery(
                "category:food payee:walmart comment:tea tag:vacation reference:123"
            )
        assertThat(
            sanitized
        ).isEqualTo("category:food payee:walmart comment:tea tag:vacation reference:123")
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
