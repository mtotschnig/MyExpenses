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

    @Test
    fun sortForNextInstanceIsNull() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenReturn(Sort.NEXT_INSTANCE.name)
        assertThat(
                Sort.preferredOrderByForTemplates(PrefKey.SORT_ORDER_TEMPLATES, prefHandler, Sort.LABEL)).isNull()
    }
    @Test
    fun sortReturnsDefault() {
        val prefHandler = mock(PrefHandler::class.java)
        `when`(prefHandler.getString(eq(PrefKey.SORT_ORDER_TEMPLATES), any())).thenAnswer { i -> i.getArguments()[1] }
        assertThat(
                Sort.preferredOrderByForTemplates(PrefKey.SORT_ORDER_TEMPLATES, prefHandler, Sort.LABEL)).isEqualTo(Sort.LABEL.toDatabaseColumn())
    }
}