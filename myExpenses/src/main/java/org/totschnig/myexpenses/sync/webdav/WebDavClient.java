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
import android.util.Log;

import org.totschnig.myexpenses.util.FileCopyUtils;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebDavClient {
    private static final String TAG = "WebDavClient";

    public static final DateFormat MODIFICATION_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    private OkHttpClient mClient;
    private Uri mBaseUri;

    public WebDavClient(String baseUrl, String userName, String password, final X509Certificate trustedCertificate) throws InvalidCertificateException {
        // Base URL needs to point to a directory.
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        mBaseUri = Uri.parse(baseUrl);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (userName != null && password != null) {
            builder.authenticator(new BasicDigestAuthenticator(mBaseUri.getHost(), userName, password));
        }

        if (trustedCertificate != null) {
            builder.sslSocketFactory(CertificateHelper.createSocketFactory(trustedCertificate));

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    try {
                        X509Certificate certificate = (X509Certificate) session.getPeerCertificates()[0];
                        return certificate.equals(trustedCertificate);
                    } catch (SSLException e) {
                        return false;
                    }
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
