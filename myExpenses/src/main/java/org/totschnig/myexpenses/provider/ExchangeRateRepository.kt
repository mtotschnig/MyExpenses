package org.totschnig.myexpenses.provider

import org.jetbrains.annotations.NotNull
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import java.io.IOException

class ExchangeRateRepository(private val dao: @NotNull ExchangeRateDao, val prefHandler: @NotNull PrefHandler,
                             val service: @NotNull ExchangeRateService) {
    @Throws(IOException::class)
    suspend fun loadExchangeRate(other: String, base: String, date: LocalDate): Float {
        val source = service.configure(prefHandler)
        return dao.getRate(base, other, date, source.name)
                ?: service.getRate(date, other, base).let {
                    dao.insert(ExchangeRate(base, other, it.first, it.second, source.name))
                    it.second
                }
    }
}