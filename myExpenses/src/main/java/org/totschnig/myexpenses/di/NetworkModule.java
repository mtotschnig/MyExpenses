package org.totschnig.myexpenses.di;


import android.net.TrafficStats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.ExchangeRateRepository;
import org.totschnig.myexpenses.retrofit.ExchangeRateService;
import org.totschnig.myexpenses.retrofit.OpenExchangeRatesApi;
import org.totschnig.myexpenses.retrofit.RatesApi;
import org.totschnig.myexpenses.room.ExchangeRateDatabase;
import org.totschnig.myexpenses.util.DelegatingSocketFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

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

import static okhttp3.logging.HttpLoggingInterceptor.Level.BASIC;
import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

@Module
class NetworkModule {

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
  static Gson provideGson() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new DateTimeDeserializer())
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

  private static class DateTimeDeserializer implements JsonDeserializer<LocalDate> {
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return LocalDate.parse(json.getAsJsonPrimitive().getAsString());
    }
  }
}
