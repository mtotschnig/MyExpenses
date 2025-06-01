package org.totschnig.myexpenses.di

import android.content.Context
import android.net.TrafficStats
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.retrofit.CoinApi
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.Frankfurter
import org.totschnig.myexpenses.retrofit.OpenExchangeRates
import org.totschnig.myexpenses.retrofit.RoadmapService
import org.totschnig.myexpenses.util.DelegatingSocketFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository.Companion.ROADMAP_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.SocketFactory

@Module
open class NetworkModule {
    @Provides
    @Singleton
    open fun provideSocketFactory(): SocketFactory =
        object : DelegatingSocketFactory(getDefault()) {
            @Throws(IOException::class)
            override fun configureSocket(socket: Socket): Socket {
                TrafficStats.setThreadStatsTag(0)
                TrafficStats.tagSocket(socket)
                return socket
            }
        }

    companion object {
        @JvmStatic
        @Singleton
        @Provides
        fun providePicasso(context: Context): Picasso = Picasso.Builder(context).build().also {
            Picasso.setSingletonInstance(it)
        }

        @JvmStatic
        @Provides
        fun provideOkHttpClientBuilder(
            loggingInterceptor: HttpLoggingInterceptor?,
            socketFactory: SocketFactory
        ) = OkHttpClient.Builder().apply {
            if (loggingInterceptor != null) {
                addInterceptor(loggingInterceptor)
            }
            if (BuildConfig.DEBUG) {
                eventListener(object : EventListener() {
                    override fun connectFailed(
                        call: Call,
                        inetSocketAddress: InetSocketAddress,
                        proxy: Proxy,
                        protocol: Protocol?,
                        ioe: IOException
                    ) {
                        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
                        CrashHandler.report(ioe)
                    }
                })
            }
            socketFactory(socketFactory)
        }

        @JvmStatic
        @Provides
        @Singleton
        fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor? = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
        } else null

        @JvmStatic
        @Provides
        @Singleton
        fun provideGson(): Gson =
            GsonBuilder()
                .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
                .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter)
                .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter)
                .create()

        @JvmStatic
        @Provides
        @Singleton
        fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory =
            GsonConverterFactory.create(gson)

        @JvmStatic
        @Provides
        @Singleton
        fun provideFrankfurter(
            builder: OkHttpClient.Builder,
            converterFactory: GsonConverterFactory
        ): Frankfurter {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.frankfurter.app/")
                .addConverterFactory(converterFactory)
                .client(builder.build())
                .build()
            return retrofit.create(Frankfurter::class.java)
        }

        @JvmStatic
        @Provides
        @Singleton
        fun provideOpenExchangeRates(
            builder: OkHttpClient.Builder,
            converterFactory: GsonConverterFactory
        ): OpenExchangeRates {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://openexchangerates.org/")
                .addConverterFactory(converterFactory)
                .client(builder.build())
                .build()
            return retrofit.create(OpenExchangeRates::class.java)
        }


        @JvmStatic
        @Provides
        @Singleton
        fun provideCoinapi(
            builder: OkHttpClient.Builder,
            converterFactory: GsonConverterFactory
        ): CoinApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://rest.coinapi.io/")
                .addConverterFactory(converterFactory)
                .client(builder.build())
                .build()
            return retrofit.create(CoinApi::class.java)
        }



        @JvmStatic
        @Provides
        @Singleton
        fun provideExchangeRateService(
            api1: Frankfurter,
            api2: OpenExchangeRates,
            api3: CoinApi
        ) = ExchangeRateService(api1, api2, api3)

        @JvmStatic
        @Provides
        @Singleton
        fun provideRoadmapService(builder: OkHttpClient.Builder, converterFactory: GsonConverterFactory): RoadmapService {
            val okHttpClient: OkHttpClient = builder
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(ROADMAP_URL)
                .addConverterFactory(converterFactory)
                .client(okHttpClient)
                .build()
            return retrofit.create(RoadmapService::class.java)
        }
    }
}