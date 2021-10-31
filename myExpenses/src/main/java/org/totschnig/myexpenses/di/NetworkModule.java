package org.totschnig.myexpenses.di;


import android.net.TrafficStats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.ExchangeRateRepository;
import org.totschnig.myexpenses.retrofit.ExchangeRateHost;
import org.totschnig.myexpenses.retrofit.ExchangeRateService;
import org.totschnig.myexpenses.retrofit.OpenExchangeRates;
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

import androidx.annotation.NonNull;
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
public class NetworkModule {

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
        public void connectFailed(@NonNull Call call, @NonNull InetSocketAddress inetSocketAddress, @NonNull Proxy proxy, Protocol protocol, @NonNull IOException ioe) {
          super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
          Timber.e(ioe, "Connect failed");
        }
      });
    }
    return builder.socketFactory(socketFactory);
  }

  @Provides
  @Singleton
  protected SocketFactory provideSocketFactory() {
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
  static ExchangeRateHost provideExchangeRateHost(OkHttpClient.Builder builder, Gson gson, Cache cache) {
    builder.cache(cache);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.exchangerate.host/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(builder.build())
        .build();
    return retrofit.create(ExchangeRateHost.class);
  }

  @Provides
  @Singleton
  static OpenExchangeRates provideOpenExchangeRates(OkHttpClient.Builder builder, Gson gson, Cache cache) {
    builder.cache(cache);
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://openexchangerates.org/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(builder.build())
        .build();
    return retrofit.create(OpenExchangeRates.class);
  }

  @Provides
  @Singleton
  static ExchangeRateService provideExchangeRateService(ExchangeRateHost api1, OpenExchangeRates api2) {
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
