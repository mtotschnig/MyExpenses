package org.totschnig.playstoreapi

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials

object AndroidPublisherHelper {

    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()

    internal fun init(): AndroidPublisher {
        val credential = ServiceAccountCredentials.getApplicationDefault()
            .createScoped("https://www.googleapis.com/auth/androidpublisher")

        return AndroidPublisher.Builder(
            NetHttpTransport.Builder().build(),
            JSON_FACTORY,
            HttpCredentialsAdapter(credential)
        ).setApplicationName(APPLICATION_NAME)
            .build()
    }
}
