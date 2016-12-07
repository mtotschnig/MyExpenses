/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package org.totschnig.myexpenses.sync.webdav;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okio.Buffer;
import okio.ByteString;

public class BasicDigestAuthenticator implements Authenticator {
    private static final String TAG = "BasicDigestAuthenticato";

    protected static final String
            HEADER_AUTHENTICATE = "WWW-Authenticate",
            HEADER_AUTHORIZATION = "Authorization";

    final String host, username, password;

    final String clientNonce;
    AtomicInteger nonceCount = new AtomicInteger(1);

    public BasicDigestAuthenticator(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;

        clientNonce = h(UUID.randomUUID().toString());
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        Request request = response.request();

        if (host != null && !request.url().host().equalsIgnoreCase(host)) {
            Log.w(TAG, "Not authenticating against " + host + " for security reasons!");
            return null;
        }

        // check whether this is the first authentication try with our credentials
        Response priorResponse = response.priorResponse();
        boolean triedBefore = priorResponse != null && priorResponse.request().header(HEADER_AUTHORIZATION) != null;

        HttpUtils.AuthScheme basicAuth = null, digestAuth = null;
        List<String> headers = response.headers(HEADER_AUTHENTICATE);
        for (HttpUtils.AuthScheme scheme : HttpUtils.parseWwwAuthenticate(headers.toArray(new String[headers.size()])))
            if ("Basic".equalsIgnoreCase(scheme.name))
                basicAuth = scheme;
            else if ("Digest".equalsIgnoreCase(scheme.name))
                digestAuth = scheme;

        // we MUST prefer Digest auth [https://tools.ietf.org/html/rfc2617#section-4.6]
        if (digestAuth != null) {
            // Digest auth

            if (triedBefore && !"true".equalsIgnoreCase(digestAuth.params.get("stale")))
                // credentials didn't work last time, and they won't work now -> stop here
                return null;

            return authorizationRequest(request, digestAuth);

        } else if (basicAuth != null) {
            // Basic auth
            if (triedBefore)    // credentials didn't work last time, and they won't work now -> stop here
                return null;

            return request.newBuilder()
                    .header(HEADER_AUTHORIZATION, Credentials.basic(username, password))
                    .build();
        } else {
            Log.w(TAG, "No supported authentication scheme");
        }

        // no supported auth scheme
        return null;
    }

    protected Request authorizationRequest(Request request, HttpUtils.AuthScheme digest) {
        String realm = digest.params.get("realm"),
                opaque = digest.params.get("opaque"),
                nonce = digest.params.get("nonce");

        Algorithm algorithm = Algorithm.determine(digest.params.get("algorithm"));
        Protection qop = Protection.selectFrom(digest.params.get("qop"));

        // build response parameters
        String response = null;

        List<String> params = new LinkedList<>();
        params.add("username=" + quotedString(username));
        if (realm != null)
            params.add("realm=" + quotedString(realm));
        else
            return null;
        if (nonce != null)
            params.add("nonce=" + quotedString(nonce));
        else
            return null;
        if (opaque != null)
            params.add("opaque=" + quotedString(opaque));

        if (algorithm != null)
            params.add("algorithm=" + quotedString(algorithm.name));

        final String method = request.method();
        final String digestURI = request.url().encodedPath();
        params.add("uri=" + quotedString(digestURI));

        if (qop != null) {
            params.add("qop=" + qop.name);
            params.add("cnonce=" + quotedString(clientNonce));

            int nc = nonceCount.getAndIncrement();
            String ncValue = String.format("%08x", nc);
            params.add("nc=" + ncValue);

            String a1 = null;
            if (algorithm == Algorithm.MD5)
                a1 = username + ":" + realm + ":" + password;
            else if (algorithm == Algorithm.MD5_SESSION)
                a1 = h(username + ":" + realm + ":" + password) + ":" + nonce + ":" + clientNonce;

            String a2 = null;
            if (qop == Protection.Auth)
                a2 = method + ":" + digestURI;
            else if (qop == Protection.AuthInt)
                try {
                    RequestBody body = request.body();
                    a2 = method + ":" + digestURI + ":" + (body != null ? h(body) : h(""));
                } catch (IOException e) {
                    Log.w(TAG, "Couldn't get entity-body for hash calculation");
                }

            if (a1 != null && a2 != null)
                response = kd(h(a1), nonce + ":" + ncValue + ":" + clientNonce + ":" + qop.name + ":" + h(a2));

        } else {
            // legacy (backwards compatibility with RFC 2069)
            if (algorithm == Algorithm.MD5) {
                String a1 = username + ":" + realm + ":" + password,
                        a2 = method + ":" + digestURI;
                response = kd(h(a1), nonce + ":" + h(a2));
            }
        }

        if (response != null) {
            params.add("response=" + quotedString(response));
            return request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Digest " + TextUtils.join(", ", params))
                    .build();
        } else
            return null;
    }

    protected String quotedString(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

    protected String h(String data) {
        return ByteString.of(data.getBytes()).md5().hex();
    }

    protected String h(@NonNull RequestBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return ByteString.of(buffer.readByteArray()).md5().hex();
    }

    protected String kd(String secret, String data) {
        return h(secret + ":" + data);
    }


    protected enum Algorithm {
        MD5("MD5"),
        MD5_SESSION("MD5-sess");

        public final String name;

        Algorithm(String name) {
            this.name = name;
        }

        static Algorithm determine(String paramValue) {
            if (paramValue == null || Algorithm.MD5.name.equalsIgnoreCase(paramValue))
                return Algorithm.MD5;
            else if (Algorithm.MD5_SESSION.name.equals(paramValue))
                return Algorithm.MD5_SESSION;
            else
                Log.w(TAG, "Ignoring unknown hash algorithm: " + paramValue);
            return null;
        }
    }

    protected enum Protection {    // quality of protection:
        Auth("auth"),              // authentication only
        AuthInt("auth-int");       // authentication with integrity protection

        public final String name;

        Protection(String name) {
            this.name = name;
        }

        static Protection selectFrom(String paramValue) {
            if (paramValue != null) {
                boolean qopAuth = false,
                        qopAuthInt = false;
                for (String qop : paramValue.split(","))
                    if ("auth".equals(qop))
                        qopAuth = true;
                    else if ("auth-int".equals(qop))
                        qopAuthInt = true;

                // prefer auth-int as it provides more protection
                if (qopAuthInt)
                    return AuthInt;
                else if (qopAuth)
                    return Auth;
            }
            return null;
        }
    }

}