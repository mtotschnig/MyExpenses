package org.totschnig.myexpenses.util.licence

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDate

data class Licence(@SerializedName("valid_since") val validSince: LocalDate?,
                   @SerializedName("valid_until") val validUntil: LocalDate?,
                   @SerializedName("type") val type: LicenceStatus?,
                   @SerializedName("features") val features: List<String>?) {
    val featureList get() = features?.joinToString(",")
}