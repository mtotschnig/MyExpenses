package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.collections.toTypedArray

@Parcelize
@Serializable
@SerialName("and")
data class AndCriterion(override val criteria: List<BaseCriterion>) : ComplexCriterion() {
    override val operator = "AND"
}