package org.totschnig.myexpenses.di;


import android.net.TrafficStats;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.DelegatingSocketFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import javax.inject.Singleton;
import javax.net.SocketFactory;

import dagger.Module;
import dagger.Provides;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BASIC;
import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

@Module
public class NetworkModule {

  @Provides
  OkHttpClient.Builder provideOkHttpClientBuilder(HttpLoggingInterceptor loggingInterceptor,
                                                  SocketFactory socketFactory) {
    final OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor);
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
  SocketFactory provideSocketFactory() {
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
  HttpLoggingInterceptor provideHttpLoggingInterceptor() {
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(BuildConfig.DEBUG ? BODY : BASIC);
    return loggingInterceptor;
  }
}
