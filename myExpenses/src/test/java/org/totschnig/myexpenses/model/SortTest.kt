package org.totschnig.myexpenses.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.prefHandler
import org.totschnig.myexpenses.preference.PrefKey

class SortTest {

    /**
     * NEXT_INSTANCE needs special treatment, because it is handled in TransactionProvider
     */
    @Test
    fun nextInstanceReturnsNull() {
        whenever(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any()))
            .thenReturn(Sort.NEXT_INSTANCE.name)
        assertThat(Sort.preferredOrderByForTemplatesWithPlans(prefHandler, Sort.TITLE))
            .isNull()
    }

    @Test
    fun configuredWithUnsupportedFallsBackToDefault() {
        whenever(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any()))
            .thenReturn(Sort.LABEL.name)
        assertThat(Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE))
            .isEqualTo("title COLLATE NOCASE")
    }

    @Test
    fun configuredWithDefault() {
        whenever(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any()))
            .thenReturn(Sort.TITLE.name)
        assertThat(Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE))
            .isEqualTo("title COLLATE NOCASE")
    }

    @Test
    fun returnsConfiguredWithSecondary() {
        whenever(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any()))
            .thenReturn(Sort.USAGES.name)
        assertThat(Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE))
            .isEqualTo("usages DESC, title COLLATE NOCASE")
    }

    @Test(expected = IllegalArgumentException::class)
    fun calledWithUnsupportedThrowsException() {
        Sort.preferredOrderByForTemplates(prefHandler, Sort.LABEL)
    }

    @Test
    fun returnsDefault() {
        whenever(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any()))
            .thenAnswer { i -> i.arguments[1] }
        assertThat(Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE))
            .isEqualTo("title COLLATE NOCASE")
    }
}