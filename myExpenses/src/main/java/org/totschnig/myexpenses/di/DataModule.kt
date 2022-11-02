package org.totschnig.myexpenses.di

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqlbrite3.SqlBrite
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandlerImpl
import javax.inject.Named
import javax.inject.Singleton

@Module
open class DataModule(private val frameWorkSqlite: Boolean = false) {
    @Provides
    @Named(AppComponent.DATABASE_NAME)
    @Singleton
    open fun provideDatabaseName() = "data"

    @Provides
    @Singleton
    fun provideSqlBrite(application: MyApplication) =
        SqlBrite.Builder().build().wrapContentProvider(
            application.contentResolver, Schedulers.io()
        )

    @Provides
    @Singleton
    open fun providePrefHandler(
        context: MyApplication,
        sharedPreferences: SharedPreferences,
        @Named(AppComponent.DATABASE_NAME) databaseName: String
    ): PrefHandler {
        return PrefHandlerImpl(context, sharedPreferences)
    }

    @Singleton
    @Provides
    open fun provideSharedPreferences(
        application: MyApplication,
        @Named(AppComponent.DATABASE_NAME) databaseName: String
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    @Singleton
    @Provides
    fun providePreferencesDataStore(appContext: MyApplication): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("UI-Settings") }
        )
    }

    @Singleton
    @Provides
    open fun provideSQLiteOpenHelperFactory(): SupportSQLiteOpenHelper.Factory =
        if (frameWorkSqlite) FrameworkSQLiteOpenHelperFactory() else
            Class.forName("io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory")
                .getConstructor().newInstance() as SupportSQLiteOpenHelper.Factory
}