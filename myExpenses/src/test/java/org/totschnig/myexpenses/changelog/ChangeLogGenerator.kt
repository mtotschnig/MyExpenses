package org.totschnig.myexpenses.changelog

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.viewmodel.data.VersionInfo


const val CURRENT_VERSION = "728;3.8.2"

@RunWith(RobolectricTestRunner::class)
class ChangeLogGenerator {

    @Test
    fun generateChangeLog() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        print(
            buildString {
                VersionInfo(CURRENT_VERSION).getChanges(context)!!.forEach {
                    appendLine(it)
                }
            }
        )
    }
}