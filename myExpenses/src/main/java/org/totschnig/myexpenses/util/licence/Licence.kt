package org.totschnig.myexpenses.util.licence

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Keep
data class Licence(
    @SerializedName("valid_since") val validSince: LocalDate?,
    @SerializedName("valid_until") val validUntil: LocalDate?,
    @SerializedName("type") val type: LicenceStatus?,
    @SerializedName("features") val features: List<String>?,
    /**
     * true if Professional Licence has expired and falls back to Extended
     */
    @SerializedName("fallback") val fallback: Boolean
) {
    val featureList
        get() = parseFeatures(features)

    fun featureListAsResIDs(context: Context) =
        parseFeatures(features).map { it.labelResId }.toIntArray()

    companion object {
        fun parseFeatures(features: List<String>?) =
            AddOnPackage.values.filter { features?.contains(it::class.simpleName) == true }
                .map { it.feature }

        fun parseFeature(feature: String) =
            AddOnPackage.values.find { it::class.simpleName.equals(feature, ignoreCase = true) }
    }
}

