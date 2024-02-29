package org.totschnig.myexpenses.retrofit

import com.google.gson.annotations.SerializedName

data class Vote(
    @SerializedName("key") val key: Pair<String, String>?,
    @SerializedName("vote") val vote: Map<Int, Int>,
    @SerializedName("email") val email: String,
    @SerializedName("version") val version: Int,
    @SerializedName("contact_consent") val contactConsent: Boolean
)