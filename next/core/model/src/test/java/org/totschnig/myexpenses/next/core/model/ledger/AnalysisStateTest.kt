package org.totschnig.myexpenses.next.core.model.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalysisStateTest {

    @Test
    fun showsResultOnlyWithConsent() {
        assertEquals(AnalysisState.AskConsent, analysisState(null, 1))
        assertEquals(AnalysisState.Declined, analysisState(false, 1))
        assertEquals(AnalysisState.Loading, analysisState<Int>(true, null))
        assertEquals(AnalysisState.Ready(1), analysisState(true, 1))
        assertEquals(1, analysisState(true, 1).valueOrNull)
        assertNull(analysisState(false, 1).valueOrNull)
    }
}
