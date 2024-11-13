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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.IOException
import java.security.GeneralSecurityException

object AndroidPublisherHelper {

    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()

    private var HTTP_TRANSPORT: HttpTransport? = null

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @return the {@Link AndroidPublisher} service
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    internal fun init(): AndroidPublisher {
        // Authorization.
        newTrustedTransport()
        val credential = ServiceAccountCredentials.getApplicationDefault().createScoped("https://www.googleapis.com/auth/androidpublisher")

        return AndroidPublisher.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, HttpCredentialsAdapter(credential) ).setApplicationName(Main.APPLICATION_NAME)
                .build()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun newTrustedTransport() {
        if (null == HTTP_TRANSPORT) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        }
    }
}