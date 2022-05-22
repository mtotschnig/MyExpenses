/*
 * Copyright 2016 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.webdav.sync.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.webdav.sync.DaggerWebDavComponent;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.LockableDavResource;
import at.bitfire.dav4android.UrlUtils;
import at.bitfire.dav4android.XmlUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.ResourceType;
import dagger.internal.Preconditions;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class WebDavClient {
  private static final String TAG = "WebDavClient";
  private static final String LOCK_TIMEOUT = String.format(Locale.ROOT, "Second-%d", 30 * 60);
  private final MediaType MIME_XML = MediaType.parse("application/xml; charset=utf-8");
  private static final String NS_WEBDAV = "DAV:";

  private final OkHttpClient httpClient;
  private final HttpUrl mBaseUri;
  private String currentLockToken;

  @Inject
  OkHttpClient.Builder builder;

  public WebDavClient(@NonNull String baseUrl, String userName, String password, final X509Certificate trustedCertificate, boolean allowUnverified) throws InvalidCertificateException {
    DaggerWebDavComponent.builder().appComponent(MyApplication.getInstance().getAppComponent()).build().inject(this);

    // Base URL needs to point to a directory.
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    mBaseUri = HttpUrl.get(baseUrl);

    int timeout = PrefKey.WEBDAV_TIMEOUT.getInt(10);

    builder.connectTimeout(timeout, TimeUnit.SECONDS);
    builder.readTimeout(timeout, TimeUnit.SECONDS);
    builder.writeTimeout(timeout, TimeUnit.SECONDS);

    if (userName != null && password != null) {
      BasicDigestAuthHandler authHandler = new BasicDigestAuthHandler(
          UrlUtils.hostToDomain(mBaseUri.host()), userName, password);
      builder.authenticator(authHandler).addNetworkInterceptor(authHandler);
    }

    if (trustedCertificate != null) {
      X509TrustManager trustManager = CertificateHelper.createTrustManager(trustedCertificate);
      SSLSocketFactory sf = CertificateHelper.createSocketFactory(trustManager);
      builder.sslSocketFactory(sf, trustManager);

      builder.hostnameVerifier((hostname, session) -> {
        try {
          X509Certificate certificate = (X509Certificate) session.getPeerCertificates()[0];
          return certificate.equals(trustedCertificate);
        } catch (SSLException e) {
          return false;
        }
      });
    } else if(allowUnverified) {
      builder.hostnameVerifier((hostname, session) -> mBaseUri.host().equals(hostname));
    }
    builder.followRedirects(false);

    httpClient = builder.build();
  }

  public void upload(String fileName, String fileContent, MediaType mediaType, DavResource parent) throws IOException, HttpException {
    new LockableDavResource(httpClient, buildResourceUri(fileName, parent.location))
        .put(RequestBody.create(mediaType, fileContent), buildIfHeader(parent.location));
  }


  public void upload(String fileName, RequestBody requestBody, DavResource parent) throws IOException, HttpException {
    new LockableDavResource(httpClient, buildResourceUri(fileName, parent.location))
        .put(requestBody, buildIfHeader(parent.location));
  }

  public void upload(String fileName, RequestBody requestBody, String folder) throws IOException, HttpException {
    new LockableDavResource(httpClient, buildResourceUri(fileName, folder))
        .put(requestBody, buildIfHeader(buildCollectionUri(folder)));
  }

  @Nullable
  private String buildIfHeader(HttpUrl folderPath) {
    if (currentLockToken == null) {
      return null;
    }
    return webdavCodedUrl(folderPath.toString()) + " " +
        webDavIfHeaderConditionList(webdavCodedUrl(currentLockToken));
  }

  public void mkCol(String folderName) throws IOException {
    LockableDavResource folder = new LockableDavResource(httpClient, buildCollectionUri(folderName));
    folder.mkColWithLock(null);
  }

  public void mkCol(String folderName, DavResource parent) throws IOException {
    LockableDavResource folder = new LockableDavResource(httpClient, buildCollectionUri(folderName, parent.location));
    folder.mkColWithLock(buildIfHeader(parent.location));
  }

  /**
   * @param folderPath if null, members of base uri are returned
   */
  public Set<DavResource> getFolderMembers(String... folderPath) throws IOException {
    return  getFolderMembers(new DavResource(httpClient, buildCollectionUri(folderPath)));
  }

  public Set<DavResource> getFolderMembers(DavResource folder) throws IOException {
    try {
      //TODO GetContentLength.NAME
      folder.propfind(1, DisplayName.NAME, ResourceType.NAME);
    } catch (DavException | HttpException e) {
      throw new IOException(e);
    }
    return folder.members;
  }

  public LockableDavResource getBase() {
    return new LockableDavResource(httpClient, mBaseUri);
  }

  public LockableDavResource getCollection(String collectionName, String... parentPath) {
    return new LockableDavResource(httpClient, buildCollectionUri(collectionName, buildCollectionUri(parentPath)));
  }

  public LockableDavResource getResource(String resourceName, String... folderPath) {
    return new LockableDavResource(httpClient, buildResourceUri(resourceName, folderPath));
  }

  public LockableDavResource getResource(HttpUrl folderUri, String resourceName) {
    return new LockableDavResource(httpClient, buildResourceUri(resourceName, folderUri));
  }

  public boolean lock(@Nullable String folderName) {
    currentLockToken = null;
    RequestBody lockXml = RequestBody.create(MIME_XML,
        "<d:lockinfo xmlns:d=\"DAV:\">\n" +
            "  <d:lockscope><d:exclusive/></d:lockscope>\n" +
            "  <d:locktype><d:write/></d:locktype>\n" +
            "  <d:owner>\n" +
            "    <d:href>http://www.myexpenses.mobi</d:href>\n" +
            "  </d:owner>\n" +
            "</d:lockinfo>");
    Request request = new Request.Builder()
        .url(buildCollectionUri(folderName))
        .header("Timeout", LOCK_TIMEOUT)
        .method("LOCK", lockXml)
        .build();
    Response response = null;
    try {
      response = httpClient.newCall(request).execute();
      if (response.isSuccessful()) {
        boolean foundTokenNode = false;

        XmlPullParser xpp = XmlUtils.newPullParser();
        xpp.setInput(response.body().charStream());
        while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
          if (xpp.getEventType() == XmlPullParser.START_TAG) {
            if (xpp.getNamespace().equals(NS_WEBDAV) && xpp.getName().equals("locktoken")) {
              foundTokenNode = true;
            } else if (foundTokenNode &&
                xpp.getNamespace().equals(NS_WEBDAV) && xpp.getName().equals("href")) {
              //TODO persist lock token
              currentLockToken = xpp.nextText();
              return true;
            }
          }
          xpp.next();
        }
      }
    } catch (IOException e) {
      Timber.w(e);
    } catch (XmlPullParserException e) {
      CrashHandler.report(e);
    } finally {
      cleanUp(response);
    }
    return false;
  }

  public boolean unlock(@Nullable String folderName) {
    Preconditions.checkNotNull(currentLockToken);
    Request request = new Request.Builder()
        .url(buildCollectionUri(folderName))
        .header("Lock-Token", webdavCodedUrl(currentLockToken))
        .method("UNLOCK", null)
        .build();
    Response response = null;
    try {
      response = httpClient.newCall(request).execute();
      currentLockToken = null;
      return response.isSuccessful();
    } catch (IOException e) {
      return false;
    } finally {
      cleanUp(response);
    }
  }

  private void cleanUp(Response response) {
    if (response != null) {
      response.close();
    }
  }

  private String webdavCodedUrl(String url) {
    return "<" + url + ">";
  }

  private String webDavIfHeaderConditionList(String condition) {
    return "(" + condition + ")";
  }

  @NonNull
  private HttpUrl buildCollectionUri(@Nullable String... folderPath) {
    if (folderPath == null) {
      return mBaseUri;
    } else {
      final HttpUrl.Builder builder = mBaseUri.newBuilder();
      for (String segment: folderPath) {
        builder.addPathSegment(segment);
      }
      return builder.addPathSegment("").build();
    }
  }

  @NonNull
  private HttpUrl buildCollectionUri(String collectionName, HttpUrl parent) {
    return parent.newBuilder().addPathSegment(collectionName).addPathSegment("").build();
  }

  @NonNull
  private HttpUrl buildResourceUri(String resourceName, @Nullable String... folderPath) {
    final HttpUrl.Builder builder = mBaseUri.newBuilder();
    if (folderPath != null) {
      for (String segment: folderPath) {
        builder.addPathSegment(segment);
      }
    }
    return builder.addPathSegment(resourceName).build();
  }

  @NonNull
  private HttpUrl buildResourceUri(String resourceName, HttpUrl parent) {
    return parent.newBuilder().addPathSegment(resourceName).build();
  }

  public void testLogin() throws IOException, HttpException, DavException {
    try {
      LockableDavResource baseResource = new LockableDavResource(httpClient, mBaseUri);
      baseResource.options();
      baseResource.propfind(1, ResourceType.NAME);
      if (!baseResource.isCollection()) {
        throw new IOException("Not a folder");
      }
      if (!baseResource.capabilities.contains("2")) {
        throw new NotCompliantWebDavException(baseResource.capabilities.contains("1"));
      }

    } catch (SSLHandshakeException e) {
      Throwable innerEx = e;
      while (innerEx != null && !(innerEx instanceof CertPathValidatorException)) {
        innerEx = innerEx.getCause();
      }

      if (innerEx != null) {
        X509Certificate cert = null;
        try {
          cert = (X509Certificate) ((CertPathValidatorException) innerEx)
              .getCertPath()
              .getCertificates()
              .get(0);
        } catch (Exception e2) {
          Timber.e(e2, "Error extracting certificate..");
        }

        if (cert != null) {
          throw new UntrustedCertificateException(cert);
        }
      }
    }
  }

  public void testClass2Locking() throws Exception {
    String folderName = "testClass2Locking";
    mkCol(folderName);
    LockableDavResource folder = new LockableDavResource(httpClient, buildCollectionUri(folderName));
    try {
      if (lock(folderName)) {
        if (unlock(folderName)) {
          if (lock(folderName)) {
            if (unlock(folderName)) {
              return;
            }
          }
        }
      }
      throw new NotCompliantWebDavException(true);
    } finally {
      try {
        folder.delete(null);
      } catch (Exception ignore) {
        //this can fail, if the unlocking mechanism does not work. We live with not being able to delete the test folder
      }
    }
  }
}
