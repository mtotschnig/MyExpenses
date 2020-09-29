package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenExchangeRatesApi {
    @GET("api/historical/{date}.json")
    fun getRate(@Path("date") date: LocalDate, @Query("symbols") symbol: String, @Query("app_id") appId: String): Call<Result>
    @Keep
    data class Result(val rates: Map<String, Float>, val timestamp: Long)
}