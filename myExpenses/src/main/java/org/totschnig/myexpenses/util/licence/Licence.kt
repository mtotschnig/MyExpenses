package org.totschnig.myexpenses.util.licence

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.ContribFeature

data class Licence(@SerializedName("valid_since") val validSince: LocalDate?,
                   @SerializedName("valid_until") val validUntil: LocalDate?,
                   @SerializedName("type") val type: LicenceStatus?,
                   @SerializedName("features") val features: List<String>?) {
    val featureList
        get() = AddOnPackage::class.sealedSubclasses.filter { features?.contains(it.simpleName) == true }
                .mapNotNull { it.objectInstance?.feature }
    val featuresAsPrefString
        get() = features?.joinToString(",")
}

fun getFeaturesFromPreference(prefString: String?) = prefString?.split(',')?.map { ContribFeature.valueOf(it) }