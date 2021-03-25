package org.totschnig.myexpenses.di;


import android.net.TrafficStats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.squareup.picasso.Picasso;

import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.ExchangeRateRepository;
import org.totschnig.myexpenses.retrofit.ExchangeRateService;
import org.totschnig.myexpenses.retrofit.OpenExchangeRatesApi;
import org.totschnig.myexpenses.retrofit.RatesApi;
import org.totschnig.myexpenses.retrofit.RoadmapService;
import org.totschnig.myexpenses.room.ExchangeRateDatabase;
import org.totschnig.myexpenses.util.DelegatingSocketFactory;
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.net.SocketFactory;

import androidx.annotation.Nullable;
import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

@Module
class NetworkModule {

  @Provides
  static Picasso providePicasso() {
    return Picasso.get();
  }

  @Provides
  static OkHttpClient.Builder provideOkHttpClientBuilder(@Nullable HttpLoggingInterceptor loggingInterceptor,
                                                         SocketFactory socketFactory) {
    final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    if (loggingInterceptor != null) {
      builder.addInterceptor(loggingInterceptor);
    }
    if (BuildConfig.DEBUG) {
      builder.eventListener(new EventListener() {
        @Override
        public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
          super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
          Timber.e(ioe, "Connect failed");
        }
      });
    }
    return builder.socketFactory(socketFactory);
  }

  @Provides
  @Singleton
  static SocketFactory provideSocketFactory() {
    return new DelegatingSocketFactory(SocketFactory.getDefault()) {
      @Override
      protected Socket configureSocket(Socket socket) throws IOException {
        TrafficStats.setThreadStatsTag(0);
        TrafficStats.tagSocket(socket);
        return socket;
      }
    };
  }

  @Provides
  @Singleton
  @Nullable
  static HttpLoggingInterceptor provideHttpLoggingInterceptor() {
    if (BuildConfig.DEBUG) {
      HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
      loggingInterceptor.setLevel(BODY);
      return loggingInterceptor;
    }
    return null;
  }

  @Provides
  @Singleton
  static Gson provideGson(JsonDeserializer<LocalDate> localDateJsonDeserializer) {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, localDateJsonDeserializer)
        .create();
  }

  @Provides
  @Singleton
  static Cache provideCache(MyApplication context) {
    return new Cache(context.getCacheDir(), 1024 * 1024);
  }

  @Provides
  @Singleton
  static RatesApi provideRatesApi(OkHttpClient.Builder builder, Gson gson, Cache cache) {
    builder.cache(cache);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.ratesapi.io/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(builder.build())
        .build();
    return retrofit.create(RatesApi.class);
  }

  @Provides
  @Singleton
  static OpenExchangeRatesApi provideOpenExchangeRatesApi(OkHttpClient.Builder builder, Gson gson, Cache cache) {
    builder.cache(cache);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://openexchangerates.org/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(builder.build())
        .build();
    return retrofit.create(OpenExchangeRatesApi.class);
  }

  @Provides
  @Singleton
  static ExchangeRateService provideExchangeRateService(RatesApi api1, OpenExchangeRatesApi api2) {
    return new ExchangeRateService(api1, api2);
  }

  @Provides
  @Singleton
  static ExchangeRateRepository provideExchangeRateRepository(
      MyApplication application, ExchangeRateService service, PrefHandler prefHandler) {
    return new ExchangeRateRepository(ExchangeRateDatabase.getDatabase(application).exchangeRateDao(), prefHandler, service);
  }

  @Provides
  @Singleton
  static JsonDeserializer<LocalDate> provideLocalDateJsonDeserializer() {
    return (json, typeOfT, context) -> LocalDate.parse(json.getAsJsonPrimitive().getAsString());
  }

  @Provides
  @Singleton
  static RoadmapService provideRoadmapService(OkHttpClient.Builder builder) {
    OkHttpClient okHttpClient = builder
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(RoadmapRepository.Companion.getROADMAP_URL())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build();
    return retrofit.create(RoadmapService.class);
  }
}
