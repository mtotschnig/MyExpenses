package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.time.ZonedDateTime

interface CoinApi {

    @GET("v1/exchangerate/{base}?invert=true")
    suspend fun getAllCurrent(
        @Path("base") base: String,
        @Query("filter_asset_id") symbols: String,
        @Header("X-CoinAPI-Key") apiKey: String
    ): AllCurrentResult

    @GET("v1/exchangerate/{base}/{symbol}")
    suspend fun getExchangeRate(
        @Path("base") base: String,
        @Path("symbol") symbol: String,
        @Query("time") time: LocalDate?,
        @Header("X-CoinAPI-Key") apiKey: String
    ): ExchangeRateResult

    @GET("v1/exchangerate/{base}/{symbol}/history?period_id=1DAY")
    suspend fun getHistory(
        @Path("base") base: String,
        @Path("symbol") symbol: String,
        @Query("time_start") start: String,
        @Query("time_end") end: String,
        @Header("X-CoinAPI-Key") apiKey: String
    ): List<HistoryResult>

    @Keep
    data class AllCurrentResult(val rates: List<ExchangeRateResult>)

    @Keep
    data class ExchangeRateResult(val time: ZonedDateTime, val rate: Double, val asset_id_quote: String)

    @Keep
    data class HistoryResult(val time_period_start: ZonedDateTime, val rate_close: Double)
}