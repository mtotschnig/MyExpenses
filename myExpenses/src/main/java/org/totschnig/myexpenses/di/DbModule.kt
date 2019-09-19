package org.totschnig.myexpenses.di

import com.squareup.sqlbrite3.SqlBrite
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import org.totschnig.myexpenses.MyApplication
import javax.inject.Singleton

@Module
class DbModule {
    @Provides
    @Singleton
    fun provideSqlBrite(application: MyApplication) = SqlBrite.Builder().build().wrapContentProvider(
            application.contentResolver, Schedulers.io())
}