package org.totschnig.myexpenses.retrofit

import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.NetworkModule
import java.time.LocalDate
import javax.net.SocketFactory

@RunWith(RobolectricTestRunner::class)
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
        runBlocking {
            val rate = service.getRate(ExchangeRateApi.OpenExchangeRates, BuildConfig.OPEN_EXCHANGE_RATES_API_KEY, date, "USD", "EUR")
            Truth.assertThat(rate.first).isEqualTo(date)
        }
    }

    @Test
    fun frankfurterIsAlive() {
        runBlocking {
            val rate = service.getRate(ExchangeRateApi.Frankfurter, null, date, "GBP", "EUR")
            Truth.assertThat(rate.first).isNotNull()
            println(rate.second.toString())
        }
    }

    @Test
    fun coinApiIsAlive() {
        Assume.assumeFalse(BuildConfig.COIN_API_API_KEY.isEmpty())
        runBlocking {
            val rate = service.getRate(ExchangeRateApi.CoinApi, BuildConfig.COIN_API_API_KEY, date, "USD", "EUR")
            Truth.assertThat(rate.first).isEqualTo(date)
            println(rate.second.toString())
        }
    }


    @Test
    fun frankfurterLatest() {
        runBlocking {
            val rates = service.getLatest(ExchangeRateApi.Frankfurter, null, "EUR", listOf("AUD", "GBP", "CHF"))
            Truth.assertThat(rates).hasSize(3)
            println(rates.joinToString())
        }
    }

    @Test
    fun openExchangeRateLatest() {
        Assume.assumeFalse(BuildConfig.OPEN_EXCHANGE_RATES_API_KEY.isEmpty())
        runBlocking {
            val rates = service.getLatest(ExchangeRateApi.OpenExchangeRates, BuildConfig.OPEN_EXCHANGE_RATES_API_KEY, "EUR", listOf("AUD", "GBP", "CHF"))
            Truth.assertThat(rates).hasSize(3)
            println(rates.joinToString())
        }
    }

    @Test
    fun coinApiRateLatest() {
        Assume.assumeFalse(BuildConfig.COIN_API_API_KEY.isEmpty())
        runBlocking {
            val rates = service.getLatest(ExchangeRateApi.CoinApi, BuildConfig.COIN_API_API_KEY, "EUR", listOf("AUD", "GBP", "CHF"))
            Truth.assertThat(rates).hasSize(3)
            println(rates.joinToString())
        }
    }
}