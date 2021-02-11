package org.totschnig.myexpenses.testutils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.SharedPreferencesModule

class TestSharedPreferencesModule: SharedPreferencesModule() {
    @Provides
    override fun provideSharedPreferences(application: Application) =
            application.getSharedPreferences(MyApplication.getTestId(), Context.MODE_PRIVATE)

}