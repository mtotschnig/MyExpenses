package org.totschnig.myexpenses.util

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel.Companion.parseUri

@RunWith(JUnitParamsRunner::class)
class ShareUtilsTest {
    @Test
    @Parameters("ftp://login:password@my.example.org:80/my/directory", "mailto:john@my.example.com")
    fun shouldParseUri(target: String) {
        Assert.assertNotNull(parseUri(target))
    }
}