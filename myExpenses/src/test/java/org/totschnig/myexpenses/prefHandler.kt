package org.totschnig.myexpenses

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey

abstract class MyAbstractClass : PrefHandler

val prefHandler: PrefHandler = mock<MyAbstractClass>().apply {
    whenever(requireString(any<PrefKey>(), any())).thenCallRealMethod()
}
