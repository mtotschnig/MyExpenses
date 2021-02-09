package org.totschnig.myexpenses

import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.testutils.Fixture
import org.totschnig.myexpenses.testutils.TestCoroutineModule
import org.totschnig.myexpenses.testutils.TestSharedPreferencesModule
import org.totschnig.myexpenses.testutils.TestViewModelModule
import java.util.*

class TestApp: MyApplication() {
    lateinit var fixture: Fixture
    override fun onCreate() {
        super.onCreate()
        fixture = Fixture(InstrumentationRegistry.getInstrumentation())
    }
    override fun buildAppComponent(systemLocale: Locale) = DaggerAppComponent.builder()
            .coroutineModule(TestCoroutineModule())
            .viewModelModule(TestViewModelModule())
            .sharedPreferencesModule(TestSharedPreferencesModule())
            .applicationContext(this)
            .systemLocale(systemLocale)
            .build()
}