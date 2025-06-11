package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface Frankfurter {
    @GET("latest")
    suspend fun getLatest(
        @Query("symbols") symbols: String,
        @Query("base") base: String
    ): Result

    @GET("{date}")
    suspend fun getHistorical(
        @Path("date") date: LocalDate,
        @Query("symbols") symbols: String,
        @Query("base") base: String
    ): Result

    @GET("{startDate}..{endDate}")
    suspend fun getTimeSeries(
        @Path("startDate") startDate: LocalDate,
        @Path("endDate") endDate: LocalDate,
        @Query("symbols") symbols: String,
        @Query("base") base: String
    ): TimeSeriesResult

    @Keep
    data class Result(
        val date: LocalDate,
        val rates: Map<String, Double>
    )

    @Keep
    data class TimeSeriesResult(
        val rates: Map<LocalDate, Map<String, Double>>
    )
}