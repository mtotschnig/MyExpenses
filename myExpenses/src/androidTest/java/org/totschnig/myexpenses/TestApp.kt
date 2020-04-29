package org.totschnig.myexpenses

import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.testutils.Fixture
import org.totschnig.myexpenses.testutils.TestCoroutineModule
import org.totschnig.myexpenses.testutils.TestViewModelModule

class TestApp: MyApplication() {
    lateinit var fixture: Fixture
    override fun onCreate() {
        super.onCreate()
        fixture = Fixture(InstrumentationRegistry.getInstrumentation())
    }
    override fun buildAppComponent() = DaggerAppComponent.builder()
            .coroutineModule(TestCoroutineModule())
            .viewModelModule(TestViewModelModule())
            .applicationContext(this)
            .build()
}