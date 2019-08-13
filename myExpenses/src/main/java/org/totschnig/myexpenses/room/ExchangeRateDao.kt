package org.totschnig.myexpenses.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.threeten.bp.LocalDate

@Dao
interface ExchangeRateDao {
    @Query("SELECT rate from exchange_rates WHERE from_currency = :from AND to_currency = :to AND date = :date ")
    suspend fun getRate(from: String, to: String, date: LocalDate): Float?


    @Insert
    suspend fun insert(exchangeRate: ExchangeRate)
}