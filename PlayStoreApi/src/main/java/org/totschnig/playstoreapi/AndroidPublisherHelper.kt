/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.totschnig.playstoreapi

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import org.apache.commons.logging.LogFactory
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Helper class to initialize the publisher APIs client library.
 *
 *
 * Before making any calls to the API through the client library you need to
 * call the [.init] method. This will run
 * all precondition checks for for client id and secret setup properly in
 * resources/client_secrets.json and authorize this client against the API.
 *
 */
object AndroidPublisherHelper {
    private val log = LogFactory.getLog(AndroidPublisherHelper::class.java)

    /** Path to the private key file (only used for Service Account auth).  */
    private const val SRC_RESOURCES_KEY_P12 = "/api-5950718857839288276-239934-55c93989bd9c.p12"

    /** Global instance of the JSON factory.  */
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()

    /** Global instance of the HTTP transport.  */
    private var HTTP_TRANSPORT: HttpTransport? = null
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun authorizeWithServiceAccount(): Credential {
        val serviceAccountEmail = Main.SERVICE_ACCOUNT_EMAIL
        log.info(String.format("Authorizing using Service Account: %s", serviceAccountEmail))

        // Build service account credential.
        return GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(setOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setServiceAccountPrivateKeyFromP12File(Main::class.java.getResourceAsStream(SRC_RESOURCES_KEY_P12))
                .build()
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @return the {@Link AndroidPublisher} service
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    internal fun init(): AndroidPublisher {
        // Authorization.
        newTrustedTransport()
        val credential = authorizeWithServiceAccount()

        // Set up and return API client.
        return AndroidPublisher.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(Main.APPLICATION_NAME)
                .build()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun newTrustedTransport() {
        if (null == HTTP_TRANSPORT) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        }
    }
}