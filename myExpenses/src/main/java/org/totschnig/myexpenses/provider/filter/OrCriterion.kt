package org.totschnig.myexpenses.provider.filter

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R

@Parcelize
@Serializable@SerialName("or")
data class OrCriterion(override val criteria: Set<Criterion>) : ComplexCriterion() {
    @IgnoredOnParcel
    override val operator = "OR"
    @IgnoredOnParcel
    override val symbol= '∨'
    @IgnoredOnParcel
    override val description = R.string.joining_or
}