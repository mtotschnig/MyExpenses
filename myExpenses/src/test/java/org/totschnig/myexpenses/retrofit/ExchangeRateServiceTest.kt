package org.totschnig.myexpenses.retrofit

import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test
import org.mockito.Mockito.mock
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.NetworkModule
import java.time.LocalDate
import javax.net.SocketFactory

class ExchangeRateServiceTest {
    private val service: ExchangeRateService = DaggerAppComponent.builder()
        .networkModule(object: NetworkModule() {
            override fun provideSocketFactory() = SocketFactory.getDefault()
        })
        .applicationContext(mock(MyApplication::class.java))
        .build().exchangeRateService()
    private val date = LocalDate.now()

    @Test
    fun openExchangeRateIsAlive() {
        Assume.assumeFalse(BuildConfig.OPEN_EXCHANGE_RATES_API_KEY.isEmpty())
        val rate = service.getRate(ExchangeRateSource.OpenExchangeRates, BuildConfig.OPEN_EXCHANGE_RATES_API_KEY, date, "USD", "EUR")
        Truth.assertThat(rate.first).isEqualTo(date)
    }

    @Test
    fun exchangeRateHostIsAlive() {
        val rate = service.getRate(ExchangeRateSource.ExchangeRateHost, null, date, "USD", "EUR")
        Truth.assertThat(rate.first).isEqualTo(date)
        println(rate.second.toString())
    }
}