package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import java.time.LocalDate
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateHost {
    @GET("timeseries")
    fun getRate(@Query("start_date") start_date: LocalDate,
                @Query("end_date") end_date: LocalDate,
                @Query("symbols") symbol: String,
                @Query("base") base: String): Call<Result>
    @Keep
    data class Result(val rates: Map<LocalDate, Map<String, Float>>)
}