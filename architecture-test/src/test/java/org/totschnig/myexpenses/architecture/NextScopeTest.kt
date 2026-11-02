package org.totschnig.myexpenses.architecture

import org.junit.Assert.assertTrue
import org.junit.Test

class NextScopeTest {

    @Test
    fun `the scope covers the shell and the modules below next`() {
        val paths = nextScope.files.map { it.projectPath }
        assertTrue(paths.any { it.startsWith("/ui_next/src/main/") })
        assertTrue(paths.any { it.startsWith("/next/core/model/src/main/") })
        assertTrue(paths.none { "/src/test/" in it || "/src/testFixtures/" in it })
    }
}
