package org.totschnig.myexpenses.testutils

import android.content.Context
import android.content.SharedPreferences
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.DataModule
import org.totschnig.myexpenses.preference.PrefHandler
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

object TestDataModule: DataModule() {
    @Provides
    @Named(AppComponent.DATABASE_NAME)
    @Singleton
    override fun provideDatabaseName() = UUID.randomUUID().toString()

    @Provides
    override fun providePrefHandler(context: MyApplication, sharedPreferences: SharedPreferences, @Named(AppComponent.DATABASE_NAME) databaseName: String): PrefHandler {
        return TestPrefHandler(context, sharedPreferences, databaseName)
    }

    @Provides
    override fun provideSharedPreferences(application: MyApplication, @Named(AppComponent.DATABASE_NAME) databaseName: String): SharedPreferences =
            application.getSharedPreferences(databaseName, Context.MODE_PRIVATE)

}