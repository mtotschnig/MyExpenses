package org.totschnig.myexpenses.provider

import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import java.io.IOException
import java.time.LocalDate

class ExchangeRateRepository(
    private val dao: @NotNull ExchangeRateDao,
    val prefHandler: @NotNull PrefHandler,
    val service: @NotNull ExchangeRateService
) {
    @Throws(IOException::class)
    suspend fun loadExchangeRate(other: String, base: String, date: LocalDate): Double {
        val source = ExchangeRateSource.preferredSource(prefHandler)
        /*        for (rate in dao.getAllRates()) {
                    Timber.d(rate.toString())
                }*/
        val apiKey = (source as? ExchangeRateSource.SourceWithApiKey)?.requireApiKey(prefHandler)
        return if (date == LocalDate.now()) {
            loadFromNetwork(source, apiKey, date, other, base).second
        } else dao.getRate(base, other, date, source.id)
            ?: loadFromNetwork(source, apiKey, date, other, base).let {
                dao.insert(ExchangeRate(base, other, it.first, it.second, source.id))
                it.second
            }
    }

    private suspend fun loadFromNetwork(
        source: ExchangeRateSource,
        apiKey: String?,
        date: LocalDate,
        other: String,
        base: String
    ) = service.getRate(source, apiKey, date, other, base)

    suspend fun deleteAll() = dao.deleteALL()
}