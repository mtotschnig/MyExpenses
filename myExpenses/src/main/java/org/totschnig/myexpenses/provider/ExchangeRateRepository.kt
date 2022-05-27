package org.totschnig.myexpenses.provider

import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import java.io.IOException
import java.time.LocalDate

class ExchangeRateRepository(private val dao: @NotNull ExchangeRateDao, val prefHandler: @NotNull PrefHandler,
                             val service: @NotNull ExchangeRateService) {
    @Throws(IOException::class)
    suspend fun loadExchangeRate(other: String, base: String, date: LocalDate): Float {
        val configuration = service.configuration(prefHandler)
/*        for (rate in dao.getAllRates()) {
            Timber.d(rate.toString())
        }*/
        return dao.getRate(base, other, date, configuration.source.name)
                ?: service.getRate(configuration, date, other, base).let {
                    dao.insert(ExchangeRate(base, other, it.first, it.second, configuration.source.name))
                    it.second
                }
    }

    suspend fun deleteAll() = dao.deleteALL()
}