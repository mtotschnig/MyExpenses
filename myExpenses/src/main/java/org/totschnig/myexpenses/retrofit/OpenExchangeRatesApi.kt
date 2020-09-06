package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface OpenExchangeRatesApi {
    @GET("api/historical/{date}.json")
    fun getRate(@Path("date") date: LocalDate, @Query("symbols") symbol: String, @Query("app_id") appId: String): Call<Result>
    @Keep
    data class Result(val rates: Map<String, Float>, val timestamp: Long)
}