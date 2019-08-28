package org.totschnig.myexpenses.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ExchangeRate::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class ExchangeRateDatabase : RoomDatabase() {

    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: ExchangeRateDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): ExchangeRateDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExchangeRateDatabase::class.java,
                    "exchange_rates.db")
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}