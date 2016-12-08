/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package org.totschnig.myexpenses.sync.webdav;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okio.Buffer;
import okio.ByteString;

/**
 * Handler to manage authentication against a given service (may be limited to one host).
 * There's no domain-based cache, because the same user name and password will be used for
 * all requests.
 *
 * Authentication methods/credentials found to be working will be cached for further requests
 * (this is why the interceptor is needed).
 *
 * Usage: Set as authenticator <b>and</b> as network interceptor.
 */
public class BasicDigestAuthHandler implements Authenticator, Interceptor {
    protected static final String
            HEADER_AUTHENTICATE = "WWW-Authenticate",
            HEADER_AUTHORIZATION = "Authorization";

    final String host, username, password;

    // cached authentication schemes
    HttpUtils.AuthScheme basicAuth, digestAuth;

    // cached digest parameters
    static String clientNonce = h(UUID.randomUUID().toString());
    static final AtomicInteger nonceCount = new AtomicInteger(1);


    public BasicDigestAuthHandler(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }


    protected Request authenticateRequest(Request request, Response response) {
        if (host != null && !request.url().host().equalsIgnoreCase(host)) {
            //Constants.log.warning("Not authenticating against " +  host + " for security reasons!");
            return null;
        }

        if (response == null) {
            // we're not processing a 401 response

            if (basicAuth == null && digestAuth == null && request.isHttps()) {
                //Constants.log.fine("Trying Basic auth preemptively");
                basicAuth = new HttpUtils.AuthScheme("Basic");
            }

        } else {
            // we're processing a 401 response

            HttpUtils.AuthScheme newBasicAuth = null, newDigestAuth = null;
            for (HttpUtils.AuthScheme scheme : HttpUtils.parseWwwAuthenticate(response.headers(HEADER_AUTHENTICATE).toArray(new String[0])))
                if ("Basic".equalsIgnoreCase(scheme.name)) {
                    if (basicAuth != null) {
                        //Constants.log.warning("Basic credentials didn't work last time -> aborting");
                        basicAuth = null;
                        return null;
                    }
                    newBasicAuth = scheme;

                } else if ("Digest".equalsIgnoreCase(scheme.name)) {
                    if (digestAuth != null && !"true".equalsIgnoreCase(scheme.params.get("stale"))) {
                        //Constants.log.warning("Digest credentials didn't work last time and server nonce has not expired -> aborting");
                        digestAuth = null;
                        return null;
                    }
                    newDigestAuth = scheme;
                }

            basicAuth = newBasicAuth;
            digestAuth = newDigestAuth;
        }

        // we MUST prefer Digest auth [https://tools.ietf.org/html/rfc2617#section-4.6]
        if (digestAuth != null) {
            //Constants.log.fine("Adding Digest authorization request for " + request.url());
            return digestRequest(request, digestAuth);

        } else if (basicAuth != null) {
            //Constants.log.fine("Adding Basic authorization header for " + request.url());

            /* In RFC 2617 (obsolete), there was no encoding for credentials defined, although
             one can interpret it as "use ISO-8859-1 encoding". This has been clarified by RFC 7617,
             which creates a new charset parameter for WWW-Authenticate, which always must be UTF-8.
             So, UTF-8 encoding for credentials is compatible with all RFC 7617 servers and many,
             but not all pre-RFC 7617 servers. */

            final String credentials = username + ":" + password;
            return request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Basic " + ByteString.of(credentials.getBytes()).base64())
                    .build();
        } else if (response != null)
            ;
            //Constants.log.warning("No supported authentication scheme");

        // no supported auth scheme
        return null;
    }

    protected Request digestRequest(Request request, HttpUtils.AuthScheme digest) {
        String  realm = digest.params.get("realm"),
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
        else {
            //Constants.log.warning("No realm provided, aborting Digest auth");
            return null;
        }
        if (nonce != null)
            params.add("nonce=" + quotedString(nonce));
        else {
            //Constants.log.warning("No nonce provided, aborting Digest auth");
            return null;
        }
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
            //Constants.log.finer("A1=" + a1);

            String a2 = null;
            if (qop == Protection.Auth)
                a2 = method + ":" + digestURI;
            else if (qop == Protection.AuthInt)
                try {
                    RequestBody body = request.body();
                    a2 = method + ":" + digestURI + ":" + (body != null ? h(body) : h(""));
                } catch(IOException e) {
                    //Constants.log.warning("Couldn't get entity-body for hash calculation");
                }
            //Constants.log.finer("A2=" + a2);

            if (a1 != null && a2 != null)
                response = kd(h(a1), nonce + ":" + ncValue + ":" + clientNonce + ":" + qop.name + ":" + h(a2));

        } else {
            //Constants.log.finer("Using legacy Digest auth");

            // legacy (backwards compatibility with RFC 2069)
            if (algorithm == Algorithm.MD5) {
                String  a1 = username + ":" + realm + ":" + password,
                        a2 = method + ":" + digestURI;
                response = kd(h(a1), nonce + ":" + h(a2));
            }
        }

        if (response != null) {
            params.add("response=" + quotedString(response));
            return request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Digest " + StringUtils.join(params, ", "))
                    .build();
        } else
            return null;
    }

    protected static String quotedString(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

    protected static String h(String data) {
        return ByteString.of(data.getBytes()).md5().hex();
    }

    protected static String h(@NonNull RequestBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return ByteString.of(buffer.readByteArray()).md5().hex();
    }

    protected static String kd(String secret, String data) {
        return h(secret + ":" + data);
    }


    protected enum Algorithm {
        MD5("MD5"),
        MD5_SESSION("MD5-sess");

        public final String name;
        Algorithm(String name) { this.name = name; }

        static Algorithm determine(String paramValue) {
            if (paramValue == null || Algorithm.MD5.name.equalsIgnoreCase(paramValue))
                return Algorithm.MD5;
            else if (Algorithm.MD5_SESSION.name.equals(paramValue))
                return Algorithm.MD5_SESSION;
            else
                //Constants.log.warning("Ignoring unknown hash algorithm: " + paramValue);
                return null;
        }
    }

    protected enum Protection {    // quality of protection:
        Auth("auth"),              // authentication only
        AuthInt("auth-int");       // authentication with integrity protection

        public final String name;
        Protection(String name) { this.name = name; }

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


    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        return authenticateRequest(response.request(), response);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (request.header(HEADER_AUTHORIZATION) == null) {
            // try to apply cached authentication
            Request authRequest = authenticateRequest(request, null);
            if (authRequest != null)
                request = authRequest;
        }
        return chain.proceed(request);
    }

}