package org.totschnig.myexpenses.provider.filter

import junit.framework.TestCase

class OperationTest : TestCase() {
    fun testGetOp() {
        assertEquals("IN ()", Operation.IN.getOp(0))
        assertEquals("IN (?)", Operation.IN.getOp(1))
        assertEquals("IN (?,?)", Operation.IN.getOp(2))
        assertEquals("IN (?,?,?)", Operation.IN.getOp(3))
    }
}