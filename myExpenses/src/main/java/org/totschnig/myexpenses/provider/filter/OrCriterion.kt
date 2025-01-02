package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable@SerialName("or")
data class OrCriterion(override val criteria: List<BaseCriterion>) : ComplexCriterion() {
    override val operator = "OR"
}