package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface OpenExchangeRates {
    @GET("api/latest.json")
    suspend fun getLatest(
        @Query("symbols") symbol: String,
        @Query("app_id") appId: String
    ): Result

    @GET("api/historical/{date}.json")
    suspend fun getHistorical(
        @Path("date") date: LocalDate,
        @Query("symbols") symbol: String,
        @Query("app_id") appId: String
    ): Result

    //Not available in free plan
/*    @GET("api/time-series.json")
    suspend fun getTimeSeries(
        @Query("app_id") apiKey: String,
        @Query("start") startDate: LocalDate,
        @Query("end") endDate: LocalDate,
        @Query("symbols") symbols: String
    ): TimeSeriesResult*/

    @Keep
    data class Result(val rates: Map<String, Double>, val timestamp: Long)

/*    @Keep
    data class TimeSeriesResult(
        val rates: Map<LocalDate, Map<String, Double>>
    )*/
}