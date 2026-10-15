package org.totschnig.myexpenses.retrofit

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateApiTest {

    @Test
    fun testFrankfurterExtractErrorJson() {
        val body = """{"message":"Error message from API"}""".toResponseBody("application/json".toMediaType())
        assertEquals("Error message from API", ExchangeRateApi.Frankfurter.extractError(body))
    }

    @Test
    fun testFrankfurterExtractErrorNonJson() {
        val body = "error".toResponseBody("text/plain".toMediaType())
        assertEquals("error", ExchangeRateApi.Frankfurter.extractError(body))
    }

    @Test
    fun testOpenExchangeRatesExtractErrorJson() {
        val body = """{"description":"Error description"}""".toResponseBody("application/json".toMediaType())
        assertEquals("Error description", ExchangeRateApi.OpenExchangeRates.extractError(body))
    }

    @Test
    fun testOpenExchangeRatesExtractErrorNonJson() {
        val body = "Internal Server Error".toResponseBody("text/plain".toMediaType())
        assertEquals("Internal Server Error", ExchangeRateApi.OpenExchangeRates.extractError(body))
    }

    @Test
    fun testCoinApiExtractErrorJson() {
        val body = """{"error":"API error"}""".toResponseBody("application/json".toMediaType())
        assertEquals("API error", ExchangeRateApi.CoinApi.extractError(body))
    }

    @Test
    fun testCoinApiExtractErrorNonJson() {
        val body = "Access Denied".toResponseBody("text/plain".toMediaType())
        assertEquals("Access Denied", ExchangeRateApi.CoinApi.extractError(body))
    }
}
