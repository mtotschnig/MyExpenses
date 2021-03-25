package org.totschnig.myexpenses.retrofit

import com.google.gson.annotations.SerializedName

data class Vote(@field:SerializedName("key") val key: String,
                @field:SerializedName("vote") val vote: Map<Int, Int>,
                @field:SerializedName("pro") val isPro: Boolean,
                @field:SerializedName("email") val email: String,
                @SerializedName("version") val version: Int)