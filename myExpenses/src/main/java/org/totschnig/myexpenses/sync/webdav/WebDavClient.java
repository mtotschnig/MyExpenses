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
import android.support.annotation.Nullable;
import android.util.Log;

import org.totschnig.myexpenses.util.AcraHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.XmlUtils;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.ResourceType;
import dagger.internal.Preconditions;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebDavClient {
  private static final String TAG = "WebDavClient";
  private static final String LOCK_TIMEOUT = String.format(Locale.ROOT, "Second-%d",30 * 60);
  private final MediaType MIME_XML = MediaType.parse("application/xml; charset=utf-8");
  private static final String NS_WEBDAV = "DAV:";

  private OkHttpClient httpClient;
  private HttpUrl mBaseUri;
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

  public void upload(String folderName, String fileName, byte[] fileContent, MediaType mediaType) throws HttpException {
    try {
      new LockableDavResource(httpClient, buildResourceUri(folderName, fileName))
          .put(RequestBody.create(mediaType, fileContent), buildIfHeader(folderName));
    } catch (IOException | at.bitfire.dav4android.exception.HttpException e) {
      throw new HttpException(e);
    }
  }

  @Nullable
  private String buildIfHeader(String folderName) {
    if (currentLockToken == null) {
      return null;
    }
    return webdavCodedUrl(buildCollectionUri(folderName).toString()) + " " +
        webDavIfHeaderConditionList(webdavCodedUrl(currentLockToken));
  }

  public void mkCol(String folderName) throws HttpException {
    LockableDavResource folder = new LockableDavResource(httpClient, buildCollectionUri(folderName));
    if (!folder.exists()) {
      try {
        folder.mkCol(null);
      } catch (IOException | at.bitfire.dav4android.exception.HttpException e) {
        throw new HttpException(e);
      }
    }
  }

  /**
   *
   * @param folderName if null, members of base uri are returned
   */
  public Set<DavResource> getFolderMembers(String folderName) throws IOException {
    DavResource folder = new DavResource(httpClient, folderName == null ? mBaseUri : buildCollectionUri(folderName));
    try {
      folder.propfind(1, DisplayName.NAME, ResourceType.NAME);
    } catch (DavException | at.bitfire.dav4android.exception.HttpException e) {
      throw new IOException(e);
    }
    return folder.members;
  }

  public LockableDavResource getResource(String folderName, String resourceName) {
    return new LockableDavResource(httpClient, buildResourceUri(folderName, resourceName));
  }

  public LockableDavResource getResource(HttpUrl folderUri, String resourceName) {
    return new LockableDavResource(httpClient, folderUri.newBuilder().addPathSegment(resourceName).build());
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
    } catch (IOException | XmlPullParserException e) {
      AcraHelper.report(e);
    } finally {
      cleanUp(response);
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
  private HttpUrl buildCollectionUri(String folderName) {
    return mBaseUri.newBuilder().addPathSegment(folderName).addPathSegment("").build();
  }

  @NonNull
  private HttpUrl buildResourceUri(String folderName, String resourceName) {
    return mBaseUri.newBuilder().addPathSegment(folderName).addPathSegment(resourceName).build();
  }

  public void testLogin() throws IOException {
    try {
      LockableDavResource baseResource = new LockableDavResource(httpClient, mBaseUri);
      baseResource.options();
      if (!baseResource.capabilities.contains("2")) {
        throw new NotCompliantWebDavException(baseResource.capabilities.contains("1"));
      }
      if (!baseResource.exists()) {
        throw new FileNotFoundException();
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
    } catch (at.bitfire.dav4android.exception.HttpException | DavException e) {
      throw new HttpException(e);
    }
  }
}
