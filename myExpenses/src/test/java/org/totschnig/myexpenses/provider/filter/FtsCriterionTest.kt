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
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        )
    }

    @Test
    fun testNullOrBlankSelectionQuery() {
        val criterion = FtsCriterion("")
        assertThat(criterion.getSelection(false)).isEqualTo("1 = 1")
    }

    @Test
    fun testSanitizeSingleWordQueryAddsWildcard() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("coffee")
        assertThat(sanitized).isEqualTo("coffee*")
    }

    @Test
    fun testSanitizeQuotedPhrasePreservesQuotes() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("\"office supplies\"")
        assertThat(sanitized).isEqualTo("\"office supplies\"")
    }

    @Test
    fun testSanitizeMultipleWordsPreservesSpace() {
        val sanitized = FtsCriterion.sanitizeFtsQuery("coffee starbucks")
        assertThat(sanitized).isEqualTo("coffee starbucks")
    }

    @Test
    fun testValuesList() {
        val criterion = FtsCriterion("coffee")
        assertThat(criterion.values).containsExactly("coffee*")
    }
}
