package org.totschnig.myexpenses.provider.filter

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@SerialName("and")
data class AndCriterion(override val criteria: List<Criterion>) : ComplexCriterion() {
    @IgnoredOnParcel
    override val operator = "AND"
}