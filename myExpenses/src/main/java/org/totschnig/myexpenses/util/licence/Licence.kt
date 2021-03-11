package org.totschnig.myexpenses.util.licence

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDate

@Keep
data class Licence(@SerializedName("valid_since") val validSince: LocalDate?,
                   @SerializedName("valid_until") val validUntil: LocalDate?,
                   @SerializedName("type") val type: LicenceStatus?,
                   @SerializedName("features") val features: List<String>?) {
    val featureList
        get() = parseFeatures(features)

    fun featureListAsResIDs(context: Context) =
            parseFeatures(features).map { it.getLabelResIdOrThrow(context) }.toTypedArray()

    companion object {
        fun parseFeatures(features: List<String>?) = AddOnPackage::class.sealedSubclasses.filter { features?.contains(it.simpleName) == true }
                .mapNotNull { it.objectInstance?.feature }

        fun parseFeature(feature: String) = AddOnPackage::class.sealedSubclasses.find { it.simpleName.equals(feature, ignoreCase = true) }?.objectInstance
    }
}

