package org.totschnig.myexpenses.di

import android.app.Application
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import javax.inject.Singleton

@Module
open class SharedPreferencesModule {
    @Singleton
    @Provides
    open fun provideSharedPreferences(application: MyApplication) = PreferenceManager.getDefaultSharedPreferences(application)
}