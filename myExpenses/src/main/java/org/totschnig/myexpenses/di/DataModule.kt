package org.totschnig.myexpenses.di

import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqlbrite3.SqlBrite
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandlerImpl
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.frameworkSupportsWindowingFunctions
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
    fun provideSQLiteOpenHelperFactory(prefHandler: PrefHandler): SupportSQLiteOpenHelper.Factory {
        if (!frameworkSupportsWindowingFunctions) {
            try {
                return Class.forName("io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory")
                    .getConstructor().newInstance() as SupportSQLiteOpenHelper.Factory
            } catch (_: Exception) { }
        }
        return FrameworkSQLiteOpenHelperFactory()
    }
}