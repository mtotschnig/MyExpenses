package org.totschnig.myexpenses

import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.testutils.TestCoroutineModule

class TestApp: MyApplication() {
    val testCoroutineModule = TestCoroutineModule()
    override fun buildAppComponent() = DaggerAppComponent.builder()
            .coroutineModule(testCoroutineModule)
            .applicationContext(this)
            .build()
}