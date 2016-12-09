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
package org.totschnig.myexpenses.sync.webdav;

import android.support.annotation.NonNull;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.common.base.Preconditions;

import org.totschnig.myexpenses.util.AcraHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.XmlUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.property.DisplayName;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebDavClient {
  private static final String TAG = "WebDavClient";
  public final MediaType MIME_XML = MediaType.parse("application/xml; charset=utf-8");
  public static final String NS_WEBDAV = "DAV:";

  public static final DateFormat MODIFICATION_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

  private OkHttpClient httpClient;
  private HttpUrl mBaseUri;
  private XmlPullParserFactory xmlPullParserFactory;
  private String currentLockToken;

  public WebDavClient(String baseUrl, String userName, String password, final X509Certificate trustedCertificate) throws InvalidCertificateException {
    // Base URL needs to point to a directory.
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    mBaseUri = HttpUrl.parse(baseUrl);

    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    if (userName != null && password != null) {
      BasicDigestAuthHandler authHandler = new BasicDigestAuthHandler(mBaseUri.host(), userName, password);
      builder.authenticator(authHandler).addNetworkInterceptor(authHandler);
    }

    if (trustedCertificate != null) {
      builder.sslSocketFactory(CertificateHelper.createSocketFactory(trustedCertificate));

      builder.hostnameVerifier((hostname, session) -> {
        try {
          X509Certificate certificate = (X509Certificate) session.getPeerCertificates()[0];
          return certificate.equals(trustedCertificate);
        } catch (SSLException e) {
          return false;
        }
      });
    }
    builder.followRedirects(false);

    httpClient = builder.build();
  }


  public void upload(String folderName, String fileName, String fileContent, MediaType mediaType) throws HttpException {
    try {
      new LockableDavResource(httpClient, buildResourceUri(folderName, fileName))
          .put(RequestBody.create(mediaType, fileContent), buildIfHeader(folderName));
    } catch (IOException | at.bitfire.dav4android.exception.HttpException e) {
      throw new HttpException(e);
    }
  }

  private String buildIfHeader(String folderName) {
    return webdavCodedUrl(buildCollectionUri(folderName).toString()) + " " +
        webDavIfHeaderConditionList(webdavCodedUrl(currentLockToken));
  }

  public String mkCol(String folderName) throws HttpException {
    HttpUrl colUri = buildCollectionUri(folderName);
    if (!colExists(colUri)) {
      Request request = new Request.Builder()
          .url(colUri)
          .method("MKCOL", null)
          .build();
      try {
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
          throw new HttpException(response);
        }
      } catch (IOException e) {
        throw new HttpException(request, e);
      }
    }
    return colUri.toString();
  }

  public Stream<DavResource> getFolderMembers(String folderName) {
    DavResource folder = new DavResource(httpClient, buildCollectionUri(folderName));
    try {
      folder.propfind(1, DisplayName.NAME);
      return Stream.of(folder.members);
    } catch (IOException | at.bitfire.dav4android.exception.HttpException | DavException e) {
      AcraHelper.report(e);
    }
    return Stream.empty();
  }

  public boolean lock(String folderName) {
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
        .method("LOCK", lockXml)
        .build();
    try {
      Response response = httpClient.newCall(request).execute();
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
    } catch (IOException | XmlPullParserException e) {
      AcraHelper.report(e);
    }
    return false;
  }

  public boolean unlock(String folderName) {
    Preconditions.checkNotNull(currentLockToken);
    Request request = new Request.Builder()
        .url(buildCollectionUri(folderName))
        .header("Lock-Token", webdavCodedUrl(currentLockToken))
        .method("UNLOCK", null)
        .build();
    try {
      Response response = httpClient.newCall(request).execute();
      return response.isSuccessful();
    } catch (IOException e) {
      return false;
    }
  }

  private String webdavCodedUrl(String url) {
    return "<" + url + ">";
  }

  private String webDavIfHeaderConditionList(String condition) {
    return "(" + condition + ")";
  }

  @NonNull
  private HttpUrl buildCollectionUri(String folderName) {
    return mBaseUri.newBuilder().addPathSegment(folderName).addPathSegment("").build();
  }

  @NonNull
  private HttpUrl buildResourceUri(String folderName, String resourceName) {
    return mBaseUri.newBuilder().addPathSegment(folderName).addPathSegment(resourceName).build();
  }

  private boolean colExists(HttpUrl colUri) throws HttpException {
    Request request = new Request.Builder()
        .url(colUri)
        .head()
        .build();
    try {
      Response response = httpClient.newCall(request).execute();
      return response.isSuccessful();
    } catch (IOException e) {
      throw new HttpException(request, e);
    }
  }

  public void testLogin() throws HttpException, UntrustedCertificateException {
    Request request = new Request.Builder()
        .url(mBaseUri.toString())
        .head()
        .build();

    try {
      Response response = httpClient.newCall(request).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response);
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
          Log.e(TAG, "Error extracting certificate..", e2);
        }

        if (cert != null) {
          throw new UntrustedCertificateException(cert);
        }
      }
    } catch (IOException e) {
      throw new HttpException(request, e);
    }
  }
}
