package org.totschnig.myexpenses.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey

class SortTest {

    /**
     * NEXT_INSTANCE needs special treatment, because it is handled in TransactionProvider
     */
    @Test
    fun nextInstanceReturnsNull() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenReturn(Sort.NEXT_INSTANCE.name)
        assertThat(
                Sort.preferredOrderByForTemplatesWithPlans(prefHandler, Sort.TITLE)).isNull()
    }

    @Test
    fun configuredWithUnsupportedFallsBackToDefault() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenReturn(Sort.LABEL.name)
        assertThat(
                Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE)).isEqualTo(Sort.TITLE.toOrderBy())
    }

    @Test
    fun configuredWithDefault() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenReturn(Sort.TITLE.name)
        assertThat(
                Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE)).isEqualTo(Sort.TITLE.toOrderBy())
    }

    @Test
    fun returnsConfiguredWithSecondary() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenReturn(Sort.USAGES.name)
        assertThat(
                Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE)).isEqualTo(
                "%s, %s".format(Sort.USAGES.toOrderBy(), Sort.TITLE.toOrderBy()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun calledWithUnsupportedThrowsException() {
        val prefHandler = mock(PrefHandler::class.java)
        Sort.preferredOrderByForTemplates(prefHandler, Sort.LABEL)
    }

    @Test
    fun returnsDefault() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenAnswer { i -> i.getArguments()[1] }
        assertThat(
                Sort.preferredOrderByForTemplates(prefHandler, Sort.TITLE)).isEqualTo(Sort.TITLE.toOrderBy())
    }
}