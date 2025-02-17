package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface Frankfurter {
    @GET("latest")
    fun getLatest(
        @Query("symbols") symbols: String,
        @Query("base") base: String
    ): Call<Result>

    @GET("{date}")
    fun getHistorical(
        @Path("date") date: LocalDate,
        @Query("symbols") symbols: String,
        @Query("base") base: String
    ): Call<Result>

    @Keep
    data class Result(
        val date: LocalDate,
        val rates: Map<String, Double>
    )
}