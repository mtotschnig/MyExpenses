package org.totschnig.myexpenses.retrofit

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClientInstance {

    private const val CONNECTION_TIME_OUT = 20L
    private const val WRITE_TIME_OUT = 20L
    private const val READ_TIME_OUT = 30L
    private var retrofit: Retrofit? = null

    fun getInstance(baseUrl: String): Retrofit {
        if (null == retrofit) {
            val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(CONNECTION_TIME_OUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIME_OUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIME_OUT, TimeUnit.SECONDS)
                    .build()

            retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(Gson()))
                    .client(okHttpClient)
                    .build()
        }

        return retrofit!!
    }

}