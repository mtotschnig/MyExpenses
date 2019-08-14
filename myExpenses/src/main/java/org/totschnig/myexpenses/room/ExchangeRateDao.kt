package org.totschnig.myexpenses.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.threeten.bp.LocalDate

@Dao
interface ExchangeRateDao {
    @Query("SELECT rate from exchange_rates WHERE from_currency = :from AND to_currency = :to AND date = :date" +
            " UNION select 1/rate from exchange_rates where from_currency = :to and to_currency = :from AND date = :date limit 1")
    suspend fun getRate(from: String, to: String, date: LocalDate): Float?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exchangeRate: ExchangeRate)
}