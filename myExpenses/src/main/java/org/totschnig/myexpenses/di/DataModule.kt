package org.totschnig.myexpenses.di

import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.preference.PreferenceManager
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
open class DataModule {
    @Provides
    @Named(AppComponent.DATABASE_NAME)
    @Singleton
    open fun provideDatabaseName() = "data"

    @Provides
    @Singleton
    fun provideSqlBrite(application: MyApplication) = SqlBrite.Builder().build().wrapContentProvider(
            application.contentResolver, Schedulers.io())

    @Provides
    @Singleton
    open fun providePrefHandler(context: MyApplication, sharedPreferences: SharedPreferences, @Named(AppComponent.DATABASE_NAME) databaseName: String): PrefHandler {
        return PrefHandlerImpl(context, sharedPreferences)
    }
    @Singleton
    @Provides
    open fun provideSharedPreferences(application: MyApplication, @Named(AppComponent.DATABASE_NAME) databaseName: String): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    @Singleton
    @Provides
    open fun provideCursorFactory() : SQLiteDatabase.CursorFactory? = null //TrackingCursorFactory()
}