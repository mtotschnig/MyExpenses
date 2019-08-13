package org.totschnig.myexpenses.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class Rates(val rates: Map<String, Float>, val base: String, val date: String)

interface ExchangeRatesApi {
    @GET("latest")
    fun getRate(@Query("symbols") symbol: String, @Query("base") base: String): Call<Rates>
}