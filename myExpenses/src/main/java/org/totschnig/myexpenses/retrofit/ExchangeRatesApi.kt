package org.totschnig.myexpenses.retrofit

import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Result(val rates: Map<String, Float>, val base: String, val date: LocalDate)

interface ExchangeRatesApi {
    @GET("api/{date}")
    fun getRate(@Path("date") date: LocalDate, @Query("symbols") symbol: String, @Query("base") base: String): Call<Result>
}