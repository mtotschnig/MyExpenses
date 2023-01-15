package org.totschnig.myexpenses.testutils

import android.content.Context
import android.content.SharedPreferences
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.DataModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

object TestDataModule: DataModule() {
    private val randomDataName: String = UUID.randomUUID().toString()

    override val databaseName: String = randomDataName

    @Provides
    override fun providePrefHandler(context: MyApplication, sharedPreferences: SharedPreferences): PrefHandler {
        return TestPrefHandler(context, sharedPreferences, randomDataName)
    }

    @Provides
    override fun provideSharedPreferences(application: MyApplication): SharedPreferences =
            application.getSharedPreferences(randomDataName, Context.MODE_PRIVATE)

    override fun providePeekHelper(): DatabaseVersionPeekHelper =
        DatabaseVersionPeekHelper { 1 }
}