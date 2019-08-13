package org.totschnig.myexpenses.provider

import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.retrofit.ExchangeRatesApi
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import java.io.IOException

class ExchangeRateRepository(val dao: ExchangeRateDao, val api: ExchangeRatesApi) {
    @Throws(IOException::class)
    suspend fun loadExchangeRate(other: String, base: String): Float {
        val date = LocalDate.now()
        var rate = dao.getRate(base, other, date)
        val error : String?
        if (rate != null) {
            return rate
        } else {
            val response = api.getRate(other, base).execute()
            if (response.isSuccessful) {
                rate = response.body()?.rates?.get(other)
                rate?.let {
                    dao.insert(ExchangeRate(base, other, date, it))
                    return it
                }
                error = "Unable to retrieve rate"
            } else {
                error = response.errorBody()?.let {
                    JSONObject(it.string()).getString("error")
                } ?: "Unknown Error"
            }
        }
        throw IOException(error)
    }
}