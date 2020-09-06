package org.totschnig.myexpenses.retrofit

import androidx.annotation.Keep
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate

interface RatesApi {
    @GET("api/{date}")
    fun getRate(@Path("date") date: LocalDate, @Query("symbols") symbol: String, @Query("base") base: String): Call<Result>
    @Keep
    data class Result(val rates: Map<String, Float>, val base: String, val date: LocalDate)
}