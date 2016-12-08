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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.FileCopyUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

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

  private OkHttpClient mClient;
  private Uri mBaseUri;
  private XmlPullParserFactory xmlPullParserFactory;
  private String currentLockToken;

  public WebDavClient(String baseUrl, String userName, String password, final X509Certificate trustedCertificate) throws InvalidCertificateException {
    // Base URL needs to point to a directory.
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    mBaseUri = Uri.parse(baseUrl);

    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    if (userName != null && password != null) {
      BasicDigestAuthHandler authHandler = new BasicDigestAuthHandler(mBaseUri.getHost(), userName, password);
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

    mClient = builder.build();
  }

  public void download(String remoteFilepath, File targetFile) throws HttpException, IOException {
    Request request = new Request.Builder()
        .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
        .build();

    Response response;
    try {
      response = mClient.newCall(request).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response);
      }
    } catch (IOException e) {
      throw new HttpException(request, e);
    }

    FileOutputStream fos = new FileOutputStream(targetFile);
    FileCopyUtils.copy(response.body().byteStream(), fos);
    fos.close();
  }

  public void upload(File localFile, String remoteFilepath, String contentType) throws HttpException {
    RequestBody body = RequestBody.create(MediaType.parse(contentType), localFile);
    Request request = new Request.Builder()
        .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
        .put(body)
        .build();

    try {
      Response response = mClient.newCall(request).execute();
      if (!response.isSuccessful()) {
        throw new HttpException(response);
      }
    } catch (IOException e) {
      throw new HttpException(request, e);
    }
  }

  public String mkCol(String folderName) throws HttpException {
    String colUri = buildCollectionUri(folderName);
    if (!colExists(colUri)) {
      Request request = new Request.Builder()
          .url(colUri)
          .method("MKCOL", null)
          .build();
      try {
        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
          throw new HttpException(response);
        }
      } catch (IOException e) {
        throw new HttpException(request, e);
      }
    }
    return colUri;
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
    String colUri = buildCollectionUri(folderName);
    Request request = new Request.Builder()
        .url(colUri)
        .method("LOCK", lockXml)
        .build();
    try {
      Response response = mClient.newCall(request).execute();
      if (response.isSuccessful()) {
        boolean foundTokenNode = false;
        xmlPullParserFactory = XmlPullParserFactory.newInstance();
        xmlPullParserFactory.setNamespaceAware(true);
        XmlPullParser xpp = xmlPullParserFactory.newPullParser();
        xpp.setInput(response.body().charStream());
        while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
          if (xpp.getEventType() == XmlPullParser.START_TAG) {
            if (xpp.getNamespace().equals(NS_WEBDAV) && xpp.getName().equals("locktoken")) {
              foundTokenNode = true;
            } else if (foundTokenNode &&
                xpp.getNamespace().equals(NS_WEBDAV) && xpp.getName().equals("href")) {
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
    String colUri = buildCollectionUri(folderName);
    Request request = new Request.Builder()
        .url(colUri)
        .header("Lock-Token", webdavCodedUrl(currentLockToken))
        .method("UNLOCK", null)
        .build();
    try {
      Response response = mClient.newCall(request).execute();
      return response.isSuccessful();
    } catch (IOException e) {
      return false;
    }
  }

  private String webdavCodedUrl(String url) {
    return "<" + url + ">";
  }

  @NonNull
  private String buildCollectionUri(String folderName) {
    return mBaseUri.buildUpon().appendPath(folderName).toString() + "/";
  }

  private boolean colExists(String colUri) throws HttpException {
    Request request = new Request.Builder()
        .url(colUri)
        .head()
        .build();
    try {
      Response response = mClient.newCall(request).execute();
      return response.isSuccessful();
    } catch (IOException e) {
      throw new HttpException(request, e);
    }
  }


  @SuppressWarnings("TryWithIdenticalCatches")
  public Date getLastModified(String remoteFilepath) throws HttpException {
    Request request = new Request.Builder()
        .url(mBaseUri.buildUpon().appendPath(remoteFilepath).toString())
        .head()
        .build();

    try {
      Response response = mClient.newCall(request).execute();
      if (response.isSuccessful()) {
        String lastModified = response.header("Last-Modified");
        if (lastModified != null) {
          return MODIFICATION_DATE_FORMAT.parse(lastModified);
        } else {
          throw new ParseException("Server did not return a Last-Modified header.", 0);
        }
      } else {
        throw new HttpException(response);
      }
    } catch (IOException e) {
      throw new HttpException(request, e);
    } catch (ParseException e) {
      throw new HttpException(request, e);
    }
  }

  public void testLogin() throws HttpException, UntrustedCertificateException {
    Request request = new Request.Builder()
        .url(mBaseUri.toString())
        .head()
        .build();

    try {
      Response response = mClient.newCall(request).execute();
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
