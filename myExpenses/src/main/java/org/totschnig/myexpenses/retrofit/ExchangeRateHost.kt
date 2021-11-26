package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate

interface ExchangeRateHost {
    @GET("latest")
    fun getLatest(
        @Query("symbols") symbol: String,
        @Query("base") base: String
    ): Call<LatestResult>

    @GET("timeseries")
    fun getTimeSeries(
        @Query("start_date") start_date: LocalDate,
        @Query("end_date") end_date: LocalDate,
        @Query("symbols") symbol: String,
        @Query("base") base: String
    ): Call<TimeSeriesResult>

    @Keep
    data class LatestResult(val rates: Map<String, Float>)

    @Keep
    data class TimeSeriesResult(val rates: Map<LocalDate, Map<String, Float>>)
}